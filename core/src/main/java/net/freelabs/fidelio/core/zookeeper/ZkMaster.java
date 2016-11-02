/*
 * Copyright (C) 2015-2016 Dionysis Lappas (dio@freelabs.net)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.freelabs.fidelio.core.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import static org.apache.zookeeper.CreateMode.EPHEMERAL;
import static org.apache.zookeeper.CreateMode.PERSISTENT;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeCreated;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;
import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

/**
 *
 * Class that provides methods to create a master process to initialize
 * zookeeper, create hierarchical namespace and set configuration data to
 * zkNodes declared in the zookeeper configuration.
 */
public final class ZkMaster extends ZkConnectionWatcher implements Runnable {

    /**
     * Zookeeper configuration.
     */
    private final ZkConf zkConf;
    /**
     * A CountDownLatch with a count of one, representing the number of events
     * that need to occur before it releases all	waiting threads.
     */
    private final CountDownLatch shutdownSignal = new CountDownLatch(1);
    /**
     * Latch set when Master process starts and is released when it initializes.
     */
    private final CountDownLatch masterInitSignal = new CountDownLatch(1);
    /**
     * Latch set when waiting for application services to closeSession.
     */
    private final CountDownLatch servicesStopped = new CountDownLatch(1);
    /**
     * A naming service instance to resolve container names to service names and
     * de-serialize data from service nodes.
     */
    private final ZkNamingService ns;
    /**
     * Indicates if there was an error during initialization of the Master
     * process. If so, then the master process is not properly initialized.
     */
    private boolean masterError;
    /**
     * List of running services.
     */
    private volatile List<String> servicesCache;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ZkMaster.class);

    /**
     * Constructor
     *
     * @param zkConf the zookeeper configuration
     */
    public ZkMaster(ZkConf zkConf) {
        // initialize super class
        super(zkConf.getZkSrvConf().getHosts(), zkConf.getZkSrvConf().getTimeout());
        // initialize sub-class
        this.zkConf = zkConf;
        this.ns = new ZkNamingService(zkConf.getServices().getPath());
        servicesCache = new ArrayList<>();
    }

    @Override
    public void run() {
        // connect to zookeeper and create a session
        connectToZk();
        // check for connection errors
        if (!masterError) {
            // Run the master process
            runMaster();
        }
    }

    /**
     * Runs the master zk process that creates zNodes, registers watches and
     * checks master initialization. If master is initialized, the {@link
     * #masterReadySignal masterReadySignal} latch is released and any callers
     * waiting on that latch may continue execution. If shutdown is detected,
     * initiates cleanup by deleting all the znodes under the app root
     * hierarchy.
     */
    public void runMaster() {
        // create zookeeper namespace for the application
        createZkNamespace();

        if (!masterError) {
            // set watch for services
            // notify callers that master finished processing
            masterInitSignal.countDown();
            // wait until it's time for shutdown
            waitForShutdown();
        } else {
            // initiate master shutdown
            shutdown();
            // notify callers that master finished processing
            masterInitSignal.countDown();
        }
    }

    /**
     * Establishes a connection with a zookeeper server and creates a new
     * session.
     */
    public void connectToZk() {
        LOG.info("Connecting to zookeeper servers...");
        try {
            connect();
        } catch (IOException ex) {
            masterError = true;
            LOG.error("Something went wrong: " + ex);
        } catch (InterruptedException ex) {
            masterError = true;
            LOG.warn("Thread Interrupted. Stopping.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates the zookeeper hierarchical namespace defined for the application.
     */
    private void createZkNamespace() {
        LOG.info("Creating application namespace in zookeeper...");
        for (ZkNode node : zkConf.getZkAppNamespace()) {
            if (!masterError) {
                createNode(node.getPath(), node.getData(), PERSISTENT);
            }
        }
    }

    /**
     * Creates a zNode.
     *
     * @param zkPath the path of the zNode.
     * @param data the data of the zNode.
     */
    private String createNode(String zkPath, byte[] data, CreateMode mode) {
        String nodePath = null;
        while (!masterError) {
            try {
                nodePath = zk.create(zkPath, data, OPEN_ACL_UNSAFE, mode);
                LOG.debug("Created zNode: " + nodePath);
                break;
            } catch (NodeExistsException e) {
                // node exists while shoudln't
                LOG.error("Node exists: " + nodePath);
                masterError = true;
            } catch (ConnectionLossException e) {
                LOG.warn("Connection loss was detected. Retrying...");
            } catch (KeeperException ex) {
                LOG.error("Something went wrong: ", ex);
                masterError = true;
            } catch (InterruptedException ex) {
                masterError = true;
                // log the event
                LOG.warn("Thread Interruped. Stopping.");
                // set the interrupt status
                Thread.currentThread().interrupt();
            }
            // check if the node was created in case of ConnectionLoss
            boolean found = checkNode(zkPath, data);
            // check if there were any errors and if the node was found
            if (found) {
                break;
            }
        }
        return nodePath;
    }

    /**
     * Checks if a zNode is created.
     *
     * @param zkPath the path of the zNode to check.
     * @param data the data of the zNode.
     */
    private boolean checkNode(String zkPath, byte[] setData) {
        while (true) {
            try {
                Stat stat = new Stat();
                byte retrievedData[] = zk.getData(zkPath, false, stat);
                /* check if this node was created by this process. In order to 
                 do so, compare the zNode's stored data with the initialization data
                 for that node.                    
                 */
                if (!Arrays.equals(retrievedData, setData)) {
                    masterError = true;
                }
                // return true that a node is created
                return true;
            } catch (NoNodeException e) {
                // no node, so try create again
                break;
            } catch (ConnectionLossException e) {
                LOG.warn("Connection loss was detected. Retrying...");
            } catch (KeeperException ex) {
                LOG.error("Something went wrong: ", ex);
                masterError = true;
                break;
            } catch (InterruptedException ex) {
                masterError = true;
                // log the event
                LOG.warn("Thread Interruped. Stopping.");
                // set the interrupt status
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    /**
     * Creates a znode using the Async API.
     *
     * @param path the path of the node to the zk namespace.
     * @param data the data of the node.
     * @param acl the access control list.
     * @param mode mode for creating the node, persistent, ephemeral etc.
     * @param cb callback object to execute when call completes.
     * @param ctx the context object.
     */
    public void createNodeAsync(String path, byte[] data, List<ACL> acl, CreateMode mode, StringCallback cb, Object ctx) {
        zk.create(path, data, acl, mode, cb, ctx);
    }

    /**
     * Gets data from a znode using the Async API.
     *
     * @param path the path of the znode to retrieve data.
     * @param watch whether need to watch this node.
     * @param cb callback object to execute when call completes.
     * @param ctx context object to be used with callback.
     */
    public void getDataAsync(String path, boolean watch, DataCallback cb, Object ctx) {
        zk.getData(path, false, cb, ctx);
    }

    /**
     * <p>
     * Checks if master is initialized.
     * <p>
     * The method waits until master initialization is finished, with or without
     * errors and checks if the {@link #masterError masterError} flag is set.
     * <p>
     * The method blocks.
     *
     * @return true if Master process is initialized without errors.
     */
    public boolean isMasterInitialized() {
        // wait until initialization is finished (with or without errors)
        waitMasterInit();
        // check for errors
        if (!masterError) {
            LOG.debug("Master is INITIALIZED.");
        } else {
            LOG.error("Zookeeper initialization for application FAILED.");
        }
        return !masterError;
    }

    /**
     * Checks if zNode exists.
     *
     * @param path the path of the zNode to check.
     * @return true if zNode exists.
     */
    public boolean nodeExists(String path) {
        while (true) {
            try {
                Stat stat = zk.exists(path, null);
                if (stat == null) {
                    LOG.error("NO {} node found.", path);
                    break;
                } else {
                    return true;
                }
            } catch (ConnectionLossException e) {
                LOG.warn("Connection loss was detected. Retrying...");
            } catch (KeeperException ex) {
                LOG.error("Something went wrong: ", ex);
                masterError = true;
                break;
            } catch (InterruptedException ex) {
                masterError = true;
                // log the event
                LOG.warn("Thread Interruped. Stopping.");
                // set the interrupt status
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    /**
     * Makes a getChildren call to application services node and registers a
     * watcher to node's children. If an error occurs, the {@link #masterError
     * masterError} flag is set to true.
     *
     * @return the list of children of application services node. An empty list
     * in case there are no children, NULL if an error occurred.
     */
    public List<String> watchServices() {
        // make a get children call to leave watch for node's children
        List<String> children = null;
        while (true) {
            try {
                children = zk.getChildren(zkConf.getServices().getPath(), childrenWatcher);
                break;
            } catch (InterruptedException ex) {
                masterError = true;
                // log event
                LOG.warn("Interrupted. Stopping");
                // set interupt flag
                Thread.currentThread().interrupt();
                break;
            } catch (ConnectionLossException ex) {
                LOG.warn("Connection loss was detected! Retrying...");
            } catch (NoNodeException ex) {
                masterError = true;
                LOG.error("Node does NOT exist: {}", ex.getMessage());
                break;
            } catch (KeeperException ex) {
                masterError = true;
                LOG.error("Something went wrong", ex);
                break;
            }
        }
        return children;
    }

    /**
     * Gets all running services of an application.
     *
     * @return a list of running services of an application. An empty list in
     * case there are no services, NULL if an error occurred.
     */
    public List<String> getRunningServices() {
        // make a get children call to leave watch for node's children
        List<String> children = null;
        while (true) {
            try {
                children = zk.getChildren(zkConf.getServices().getPath(), null);
                break;
            } catch (InterruptedException ex) {
                // log event
                LOG.warn("Interrupted. Stopping");
                // set interupt flag
                Thread.currentThread().interrupt();
                break;
            } catch (ConnectionLossException ex) {
                LOG.warn("Connection loss was detected! Retrying...");
            } catch (NoNodeException ex) {
                LOG.error("Node does NOT exist: {}", ex.getMessage());
                break;
            } catch (KeeperException ex) {
                LOG.error("Something went wrong", ex);
                break;
            }
        }
        return children;
    }

    /**
     * A watcher to activate when change in registered node's children happens.
     */
    public final Watcher childrenWatcher = (WatchedEvent event) -> {
        LOG.debug(event.getType() + ", " + event.getPath());
        // re-set watch
        List<String> children = watchServices();

        // if no error
        if (children != null) {
            if (event.getType() == NodeChildrenChanged) {
                // if no children exist
                if (children.isEmpty()) {
                    printStoppedSrvs(children, servicesCache);
                    LOG.info("All services stopped.");
                    servicesStopped.countDown();
                } else {
                    // services still exist, check which service(s) stopped
                    printStoppedSrvs(children, servicesCache);
                }
                servicesCache = children;
            }
        } else {
            LOG.error("Received INVALID list of running services.");
            servicesStopped.countDown();
        }
    };

    /**
     * Prints stopped services. The method accepts two lists. The srcList is
     * checked against the trgList. If srcList has less elements than the
     * trgList, them the elements of trgList that are not included in srcList
     * are printed.
     *
     * @param srcList the list of elements to check.
     * @param trgList the list of elements to check against.
     */
    private void printStoppedSrvs(List<String> srcList, List<String> trgList) {
        // if there are nodes not included in srcList
        if (!srcList.containsAll(trgList)) {
            // create the difference of lists
            List<String> diff = trgList;
            // get the difference of two lists
            diff.removeAll(srcList);
            // print diff
            for (String srv : diff) {
                String[] tokens = srv.split("/");
                int size = tokens.length;
                String name = tokens[size - 1];
                LOG.info("Service {} stopped.", name);
            }
        }
    }

    /**
     * Waits until all application services have stopped or a timeout occurs,
     * whichever happens first.
     *
     * @param services list of services to wait to closeSession.
     * @param timeout the maximum time to wait
     * @param timeUnit the time unit of the timeout argument
     * @return true if all services stopped. False in case an error occurred.
     */
    public boolean waitServicesToStop(List<String> services, long timeout, TimeUnit timeUnit) {
        boolean stopped = false;
        LOG.info("Stopping services...");
        // get services running
        servicesCache = services;

        if (servicesCache != null) {
            if (!servicesCache.isEmpty()) {
                try {
                    stopped = servicesStopped.await(timeout, timeUnit);
                    if (!stopped) {
                        LOG.warn("Some services are still running!");
                    } else {
                        // sleep for 1s cause if containers's state is checked immediately
                        // state of the last one may be found running but it will have terminated
                        // as soon as the stop request is posted
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException ex) {
                    masterError = true;
                    // log the event
                    LOG.warn("Thread Interruped. Stopping.");
                    // set the interrupt status
                    Thread.currentThread().interrupt();
                }
            } else {
                LOG.error("Containers-Services NOT running.");
                masterError = true;
            }
        } else {
            LOG.error("An error encountered while waiting for services to stop.");
        }

        return !masterError && stopped;
    }

    /**
     * Gets data from a zNode.
     *
     * @param path the path of the zNode to get data.
     * @param stat object to be filled with metadata of the requested zNode.
     * @return data of the zNode. Null in case of error.
     */
    public byte[] nodeData(String path, Stat stat) {
        while (true) {
            try {
                byte[] data = zk.getData(path, false, stat);
                return data;
            } catch (NoNodeException e) {
                masterError = true;
                break;
            } catch (ConnectionLossException e) {
                LOG.warn("Connection loss was detected. Retrying...");
            } catch (KeeperException ex) {
                LOG.error("Something went wrong: ", ex);
                masterError = true;
                break;
            } catch (InterruptedException ex) {
                masterError = true;
                // log the event
                LOG.warn("Thread Interruped. Stopping.");
                // set the interrupt status
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    /**
     * Sets watch for services.
     */
    private void setSrvWatch() {
        // iterate through containers map and get container names
        for (String conName : zkConf.getContainers().keySet()) {
            // get the zNode path of the service offered by the container
            String srvPath = ns.resolveSrvName(conName);
            // set watch
            srvExists(srvPath);
        }
    }

    /**
     * Initiates a zk exists operation on a service zNode.
     *
     * @param zkPath
     */
    private void srvExists(String zkPath) {
        zk.exists(zkPath, srvExistsnWatcher, srvExistsCallback, null);
    }

    /**
     * Callback to be used with {@link #srvExists() srvExists()} method.
     */
    private final StatCallback srvExistsCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                srvExists(path);
                break;
            case NONODE:
                LOG.info("Watch registered on: " + path);
                break;
            case OK:
                LOG.error("Node exists: " + path);
                masterError = true;
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * A watcher to process a watch notification for shutdown node.
     */
    private final Watcher srvExistsnWatcher = (WatchedEvent event) -> {
        LOG.info(event.getType() + ", " + event.getPath());

        if (event.getType() == NodeCreated) {
            getSrvData(event.getPath());
        }
    };

    /**
     * Get data from the requested service zNode.
     *
     * @param zkPath the path of the service to the zookeeper namespace.
     */
    private void getSrvData(String zkPath) {
        zk.getData(zkPath, getSrvDataWatcher, getSrvDataCallback, null);
    }

    /**
     * The callback to be used with
     * {@link #getSrvData(java.lang.String) getSrvData} method.
     */
    private final DataCallback getSrvDataCallback = (int rc, String path, Object ctx, byte[] data, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected");
                getSrvData(path);
                break;
            case NONODE:
                LOG.error("No service: " + path);
                break;
            case OK:
                // process retrieved data from requested service zNode
                processSrvData(data, path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * <p>
     * Processes data retrieved from a service zNode.
     * <p>
     * De-serializes the service node, gets the zNode path of the container
     * offering that service.
     *
     * @param data the data from a service zNode to process.
     * @param path the path of the service zNode.
     */
    private void processSrvData(byte[] data, String srvPath) {
        // de-serialize service node
        ZkNamingServiceNode node = ns.deserializeZkSrvNode(srvPath, data);
        LOG.info("Service update: {}\tStatus: {}", srvPath, node.getStatus().toString());
    }

    /**
     * Watcher to be used with {@link  #getSrvData(java.lang.String) getSrvData}
     * method.
     */
    private final Watcher getSrvDataWatcher = (WatchedEvent event) -> {
        if (event.getType() == NodeDataChanged) {
            getSrvData(event.getPath());
        }
    };

    /**
     * <p>
     * Returns a list of all the nodes of a zookeeper namespace hierarchy.
     * <p>
     * The method is called with a rootNode argument and returns a list of all
     * the nodes under this rootNode (including rootNode). The children of every
     * node are returned first!
     *
     * @param node the root node of the zookeeper namespace for the app to
     * populate hierarchy.
     * @return a list of all the nodes of the hierarchy defined under the param
     * rootNode (including rootNode).
     */
    private List<String> getAllNodes(String node) {
        // a list to hold the returned nodes
        List<String> allNodes = new ArrayList<>();
        // get the children of the node
        List<String> children = null;
        while (true) {
            try {
                children = zk.getChildren(node, false);
                break;
            } catch (InterruptedException ex) {
                // log event
                LOG.warn("Interrupted. Stopping");
                // set interupt flag
                Thread.currentThread().interrupt();
                masterError = true;
                break;
            } catch (ConnectionLossException ex) {
                LOG.warn("Connection loss was detected! Retrying...");
            } catch (NoNodeException ex) {
                LOG.info("Node already deleted: {}", ex.getMessage());
                break;
            } catch (KeeperException ex) {
                LOG.error("Something went wrong", ex);
                masterError = true;
                break;
            }
        }
        // if node has children, for every child recurse and add returned node to list
        if (children != null) {
            if (!children.isEmpty()) {
                for (String child : children) {
                    List<String> tmpList = getAllNodes(node + "/" + child);
                    if (tmpList != null) {
                        allNodes.addAll(tmpList);
                    }
                }
            }
            // add the node with wich the method was called to the list
            allNodes.add(node);
        }
        return allNodes;
    }

    /**
     * Cleans the zookeeper namespace from all the nodes created by the
     * application.
     *
     * @return true if operation completed without errors.
     */
    public boolean cleanZkNamespace() {
        LOG.info("Cleaning zookeeper namespace...");

        List<String> nodesToDelete = getAllNodes(zkConf.getRoot().getPath());
        ListIterator<String> iter = nodesToDelete.listIterator();

        while (iter.hasNext()) {
            // get a node
            String node = iter.next();
            // delete the node, don't check for version
            deleteNode(node, -1);
        }
        return !masterError;
    }

    /**
     * Deletes the specified zNode. The zNode mustn't have any children.
     *
     * @param path the zNode to delete.
     * @param version the data version of the zNode.
     */
    public void deleteNode(String path, int version) {
        while (true) {
            try {
                zk.delete(path, version);
                LOG.debug("Deleted node: {}", path);
                break;
            } catch (InterruptedException ex) {
                // log event
                LOG.warn("Interrupted. Stopping.");
                // set interupt flag
                Thread.currentThread().interrupt();
                masterError = true;
                break;
            } catch (ConnectionLossException ex) {
                LOG.warn("Connection loss was detected. Retrying...");
            } catch (NoNodeException ex) {
                LOG.info("Node already deleted: {}", path);
                break;
            } catch (KeeperException ex) {
                LOG.error("Something went wrong", ex);
                masterError = true;
                break;
            }
        }
    }

    /**
     * Checks the data from a zkNode. FOR DEBUGGIND.
     *
     * @param path the path of the znode to check.
     * @return the size of the znode data in bytes.
     * @throws org.apache.zookeeper.KeeperException error handling from zk.
     * @throws java.lang.InterruptedException if interrupted.
     */
    public byte[] checkData(String path) throws KeeperException, InterruptedException {
        byte[] data = zk.getData(path, false, null);
        LOG.info("Data length is: " + data.length);
        LOG.info("Data is: " + new String(data));
        return data;
    }

    /**
     * Sets data to container node.
     *
     * @param path the path of the node to set data.
     * @param data the data to be set to the node.
     */
    public void setNodeDataAsync(String path, byte[] data) {
        zk.setData(path, data, -1, setNodeDataAsyncCallback, data);
    }

    /**
     * Callback object to be used with (@link #setNodeDataAsync()
     * setNodeDataAsync) method.
     */
    private final StatCallback setNodeDataAsyncCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                setNodeDataAsync(path, (byte[]) ctx);
                break;
            case NONODE:
                LOG.error("Znode does not exist: " + path);
                break;
            case OK:
                LOG.info("Data set to zNode: " + path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(Code.get(rc), path));
                break;
        }
    };

    public boolean setNodeDataSync(String path, byte[] data) {
        boolean success = false;
        while (true) {
            try {
                zk.setData(path, data, -1);
                success = true;
                break;
            } catch (InterruptedException ex) {
                // log event
                LOG.warn("Interrupted. Stopping");
                // set interupt flag
                Thread.currentThread().interrupt();
                break;
            } catch (ConnectionLossException ex) {
                LOG.warn("Connection loss was detected! Retrying...");
            } catch (NoNodeException ex) {
                LOG.info("No zNode {} to set data.", path);
                break;
            } catch (KeeperException ex) {
                LOG.error("Something went wrong", ex);
                break;
            }
        }
        return success;
    }

    /**
     * Blocks until shutdown.
     *
     */
    public void waitForShutdown() {
        try {
            shutdownSignal.await();
        } catch (InterruptedException ex) {
            // log the event
            LOG.warn("Thread Interruped. Stopping.");
            // set the interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Initiates shutdown.
     */
    public void shutdown() {
        LOG.info("Initiating master shutdown.");
        // delete nanespace
        cleanZkNamespace();
        try {
            // close session
            closeSession();
        } catch (InterruptedException ex) {
            // log the event
            LOG.warn("Thread Interruped. Stopping.");
            // set the interrupt status
            Thread.currentThread().interrupt();
        }
        // release latch to finish execution
        shutdownSignal.countDown();
    }

    /**
     * <p>
     * Waits until the Master process initializes.
     * <p>
     * Method blocks.
     */
    private void waitMasterInit() {
        try {
            masterInitSignal.await();
        } catch (InterruptedException ex) {
            // log event
            LOG.warn("Interrupted. Stopping.");
            // set interupt flag
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Initiates master process shutdown without affecting the state of the
     * deployed application.
     */
    public void shutdownMaster() {
        LOG.debug("Initiating master shutdown.");
        try {
            // close session
            closeSession();
        } catch (InterruptedException ex) {
            // log the event
            LOG.warn("Thread Interruped. Stopping.");
            // set the interrupt status
            Thread.currentThread().interrupt();
        }
        // release latch to finish execution
        shutdownSignal.countDown();
    }

    /**
     * Creates a zookeeper node to the application namespace that indicates to
     * the application components to initiate shutdown.
     */
    public void createShutdownNode() {
        createNode(zkConf.getShutdown().getPath(), zkConf.getShutdown().getData(), EPHEMERAL);
    }

    /**
     * Deletes a zookeeper node to the application namespace that indicates to
     * the application components to initiate shutdown.
     */
    public void deleteShutdownNode() {
        deleteNode(zkConf.getShutdown().getPath(), -1);
    }

    /**
     * Signals the application components to initiate shutdown process.
     */
    public void signalAppShutdown() {
        createShutdownNode();
        deleteShutdownNode();
    }

    /**
     *
     * @return the id of the root zookeeper node of the deployed application
     * namespace.
     */
    public String getDeployedID() {
        return zkConf.getRoot().getName();
    }

    public void display() {
        try {
            List<String> children = zk.getChildren(zkConf.getRoot().getPath(), false);
            LOG.info("Printing children of ROOT.");
            for (String child : children) {
                LOG.info("Node: " + child);
            }
        } catch (KeeperException | InterruptedException ex) {
            LOG.error("Something went wrong: ", ex);
        }
    }

    public boolean isMasterError() {
        return masterError;
    }

    public ZooKeeper getZk() {
        return this.zk;
    }
}
