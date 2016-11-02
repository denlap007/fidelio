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
package net.freelabs.fidelio.broker;

import net.freelabs.fidelio.broker.process.start.MainProcessHandler;
import net.freelabs.fidelio.broker.process.start.MainProcessData;
import net.freelabs.fidelio.broker.process.start.StartResMapper;
import net.freelabs.fidelio.broker.env.EnvironmentMapper;
import net.freelabs.fidelio.broker.env.EnvironmentHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.bind.JAXBException;
import net.freelabs.fidelio.broker.process.DefaultProcessHandler;
import net.freelabs.fidelio.broker.process.Executable;
import net.freelabs.fidelio.broker.process.ProcessData;
import net.freelabs.fidelio.broker.process.ProcessHandler;
import net.freelabs.fidelio.broker.process.ProcessManager;
import net.freelabs.fidelio.broker.process.Resource;
import net.freelabs.fidelio.broker.process.ResourceMapper;
import net.freelabs.fidelio.broker.process.start.StartGroupProcessHandler;
import net.freelabs.fidelio.broker.process.stop.StopGroupProcessHandler;
import net.freelabs.fidelio.broker.process.stop.StopResMapper;
import net.freelabs.fidelio.broker.services.ServiceManager;
import net.freelabs.fidelio.broker.shutdown.Shutdown;
import net.freelabs.fidelio.broker.shutdown.ShutdownNotifier;
import net.freelabs.fidelio.broker.tasks.TaskHandler;
import net.freelabs.fidelio.broker.tasks.TaskMapper;
import net.freelabs.fidelio.core.schema.BusinessContainer;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.schema.ContainerEnvironment;
import net.freelabs.fidelio.core.schema.DataContainer;
import net.freelabs.fidelio.core.schema.StartRes;
import net.freelabs.fidelio.core.schema.StopRes;
import net.freelabs.fidelio.core.schema.Tasks;
import net.freelabs.fidelio.core.schema.WebContainer;
import net.freelabs.fidelio.core.serializer.JAXBSerializer;
import net.freelabs.fidelio.core.zookeeper.ZkConnectionWatcher;
import net.freelabs.fidelio.core.zookeeper.ZkNamingService;
import net.freelabs.fidelio.core.zookeeper.ZkNamingServiceNode;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeCreated;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted;
import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Class that defines a Broker client to the zookeeper configuration store.
 */
public abstract class Broker extends ZkConnectionWatcher implements Shutdown, Lifecycle {

    /**
     * The path of the Container to the zookeeper namespace.
     */
    private final String zkContainerPath;
    /**
     * Initial data for the zookeeper Broker node.
     */
    private static final String BROKER_ID = Long.toString(new Random().nextLong());
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Broker.class);
    /**
     * The path of the zNode that indicates the applications shutdown.
     */
    private final String shutdownNode;
    /**
     * The path of the zNode that holds the initial configuration for the
     * container.
     */
    private final String conConfNode;
    /**
     * An object to handle execution of operations on another thread.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    /**
     * The name of the container associated with the broker.
     */
    private final String conSrvName;
    /**
     * The container associated with the broker. Holds the configuration.
     */
    private Container container;
    /**
     * The znode of the container service to the naming service.
     */
    private final ZkNamingServiceNode conZkSrvNode;
    /**
     * Service Manager. Stores all the data for services-dependencies of the
     * container.
     */
    private ServiceManager srvMngr;
    /**
     * Handles the interaction with the naming service.
     */
    private final ZkNamingService ns;
    /**
     * Manages process execution.
     */
    private ProcessManager procMngr;
    /**
     * Handles the environment creation of container processes.
     */
    private EnvironmentHandler envHandler;
    /**
     * Handler tasks execution.
     */
    private TaskHandler taskHandler;
    /**
     * Blocks/Un-blocks execution for shutdown.
     */
    public static final ShutdownNotifier SHUTDOWN = new ShutdownNotifier();
    /**
     * Configuration for the program.
     */
    private final BrokerConf brokerConf;
    /**
     * Handles execution of container life-cycles based on events.
     */
    private final LifecycleHandler lifecycleHandler = new LifecycleHandler();
    /**
     * Defines an action to execute when a dependent service shuts down.
     */
    private Executable execOnDependentSrvShutdown;

    /**
     * Constructor
     *
     * @param zkHosts the zookeeper hosts list.
     * @param zkSessionTimeout the client session timeout.
     * @param zkContainerPath the path of the Container to the zookeeper
     * namespace.
     * @param zkNamingService the path of the naming service to the zookeeper
     * namespace.
     * @param shutdownNode the node the signals the shutdown.
     * @param conConfNode the node with the initial container configuration.
     */
    public Broker(String zkHosts, int zkSessionTimeout, String zkContainerPath, String zkNamingService, String shutdownNode, String conConfNode) {
        super(zkHosts, zkSessionTimeout);
        this.zkContainerPath = zkContainerPath;
        this.shutdownNode = shutdownNode;
        this.conConfNode = conConfNode;
        conSrvName = resolveConPath(zkContainerPath);
        brokerConf = new BrokerConf();
        brokerConf.brokerDir = BrokerConf.SERVICES_DIR + File.separator + conSrvName + "-service";
        // create a new naming service node
        conZkSrvNode = new ZkNamingServiceNode(zkContainerPath);
        // initialize the naming service object
        ns = new ZkNamingService(zkNamingService);
        // stores the context data of the particular thread for logging
        MDC.put("id", conSrvName);
    }

    /*
     * *************************************************************************
     * BOOTSTRAPPING
     * **************************************************************************
     */
    public void entrypoint() {
        // initialize lifecycle handler
        lifecycleHandler.setExecContainerBootCycle(() -> executorService.execute(() -> {
            boot();
        }));
        lifecycleHandler.setExecContainerInitCycle(() -> executorService.execute(() -> {
            init();
        }));
        lifecycleHandler.setExecContainerStartLifeCycle(() -> executorService.execute(() -> {
            start();
        }));
        lifecycleHandler.setExecContainerShutdownLifeCycle(() -> executorService.execute(() -> {
            shutdown();
        }));
        lifecycleHandler.setExecContainerUpdateLifeCycle(() -> executorService.execute(() -> {
            update();
        }));
        lifecycleHandler.setExecContainerErrorLifeCycle(() -> executorService.execute(() -> {
            error();
        }));
        // send the bootEvent
        lifecycleHandler.bootEvent();
    }

    /**
     * Bootstraps the broker.
     */
    @Override
    public void boot() {
        LOG.info("Starting program boot.");
        // connect to zookeeper
        boolean connected = connectToZk();
        // if succeeded
        if (connected) {
            // start initialization
            lifecycleHandler.containerInitEvent();
        } else {
            LOG.error("FAILED to start broker. Terminating.");
            errExit();
        }
    }

    /**
     * Exits with error code -1.
     */
    private void errExit() {
        System.exit(-1);
    }

    /**
     * Establishes a connection with a zookeeper server and creates a new
     * session.
     *
     * @return true if connected successfully.
     */
    private boolean connectToZk() {
        boolean connected = false;
        try {
            LOG.info("Connecting to zk servers...");
            connect();
            connected = true;
            registerNewZkConnectionWatcher(zkHanleReconnectionWatcher);
        } catch (IOException ex) {
            LOG.error("Something went wrong: " + ex);
        } catch (InterruptedException ex) {
            LOG.warn("Thread Interrupted. Stopping.");
            Thread.currentThread().interrupt();
        }
        return connected;
    }

    /**
     * ****************************
     * SESSION EXPIRED HANDLING
     * ****************************
     */
    /**
     * A watcher for the zk connection to handle session expired event.
     */
    private final Watcher zkHanleReconnectionWatcher = new Watcher() {
        @Override
        public void process(WatchedEvent event) {
            LOG.info("SESSION STATE EVENT: {}", event.getState());

            if (event.getState() == Event.KeeperState.Expired) {
                // create new session
                connectToZk();
                // re-register to naming service
                String path = ns.resolveSrvName(conSrvName);
                reCreateZkConSrvNode(path, new byte[0]);
                // re-set watch for shutdown
                reSetShutDownWatch();
            }
        }
    };

    /**
     * Re-creates the service node for this container to the naming service.
     *
     * @param path the path of the zNode to the zookeeper namespace.
     * @param data the data of the zNode.
     */
    private void reCreateZkConSrvNode(String path, byte[] data) {
        zk.create(path, data, OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL, recreateZkConSrvNodeCallback, data);
    }

    /**
     * The object to call back with {@link #reCreateZkConSrvNode(java.lang.String, byte[])
     * createZkConSrvNode} method.
     */
    private final StringCallback recreateZkConSrvNodeCallback = (int rc, String path, Object ctx, String name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                checkContainerNode(path, (byte[]) ctx);
                break;
            case NODEEXISTS:
                LOG.error("Service zNode already exists: " + path);
                break;
            case OK:
                LOG.info("Re-registered to naming service: " + path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Re-sets a watch on the zookeeper shutdown node. When the shutdown zNode
     * is created execution is terminated.
     */
    private void reSetShutDownWatch() {
        zk.exists(shutdownNode, shutDownWatcher, resetshutDownCallback, null);
    }

    /**
     * Callback to be used with {@link #setShutDownWatch() setShutDownWatch()}
     * method.
     */
    private final StatCallback resetshutDownCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                reSetShutDownWatch();
                break;
            case NONODE:
                LOG.info("Watch registered on: " + path);
                break;
            case OK:
                LOG.info("Shutdown node found: " + path);
                lifecycleHandler.shutdownEvent();
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /*
     * *************************************************************************
     * INITIALIZATION
     * **************************************************************************
     */
    @Override
    public void init() {
        LOG.info("Starting container initialization.");
        // set watch for shutdown zNode
        setShutDownWatch();
        // create container zNode
        createZkNodeEphemeral(zkContainerPath, BROKER_ID.getBytes());
        // set watch for the container description
        waitForConDescription();
    }

    /**
     * Sets a watch on the zookeeper shutdown node. When the shutdown zNode is
     * created execution is terminated.
     */
    private void setShutDownWatch() {
        zk.exists(shutdownNode, shutDownWatcher, shutDownCallback, null);
    }

    /**
     * Callback to be used with {@link #setShutDownWatch() setShutDownWatch()}
     * method.
     */
    private final StatCallback shutDownCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                setShutDownWatch();
                break;
            case NONODE:
                LOG.info("Watch registered on: " + path);
                break;
            case OK:
                LOG.error("Node exists: " + path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * A watcher to process a watch notification for shutdown node.
     */
    private final Watcher shutDownWatcher = (WatchedEvent event) -> {
        LOG.info("WATCH triggered. Type {} for {}", event.getType(), event.getPath());

        if (event.getType() == NodeCreated) {
            lifecycleHandler.shutdownEvent();
        }
    };

    /**
     * Creates an EPHEMERAL zNode.
     *
     * @param path the path of the zNode to the zookeeper namespace.
     * @param data the data of the zNode.
     */
    private void createZkNodeEphemeral(String path, byte[] data) {
        zk.create(path, data, OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL, createZkNodeEphemeralCallback, data);
    }

    /**
     * The object to call back with {@link #createZkNodeEphemeral(java.lang.String, byte[])
     * createZkNodeEphemeral} method.
     */
    private final StringCallback createZkNodeEphemeralCallback = (int rc, String path, Object ctx, String name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                checkContainerNode(path, (byte[]) ctx);
                break;
            case NODEEXISTS:
                LOG.error("Node already exists: " + path);
                break;
            case OK:
                LOG.info("Created zNode: " + path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Checks weather the container zNode was created or not.
     */
    private void checkContainerNode(String path, byte[] data) {
        zk.getData(path, false, checkContainerNodeCallback, data);
    }

    /**
     * The object to call back with {@link #checkContainerNode(java.lang.String, byte[])
     * checkContainerNode} method.
     */
    private final DataCallback checkContainerNodeCallback = (int rc, String path, Object ctx, byte[] data, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                checkContainerNode(path, (byte[]) ctx);
                break;
            case NONODE:
                createZkNodeEphemeral(path, (byte[]) ctx);
                break;
            case OK:
                String originalData = new String((byte[]) ctx);
                String foundData = new String(data);
                // check if this zNode is created by this client
                if (foundData.equals(originalData) == true) {
                    LOG.info("Created zNode: " + path);
                } else {
                    LOG.error("Cannot create zNode. Node already exists: " + path);
                }
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Checks if the container description for this container exists and sets a
     * watch.
     */
    private void waitForConDescription() {
        zk.exists(conConfNode, waitForConDescriptionWatcher, waitForConDescriptionCallback, null);
    }

    /**
     * The object to call back with
     * {@link #waitForConDescription() waitForConDescription} method.
     */
    private final AsyncCallback.StatCallback waitForConDescriptionCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                waitForConDescription();
                break;
            case NONODE:
                LOG.info("Waiting for container description: " + path);
                break;
            case OK:
                LOG.info("Container description found: " + path);
                // get the description from the configuration zNode
                getConDescription();
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * A watcher to process a watch notification for configuration node.
     */
    private final Watcher waitForConDescriptionWatcher = (WatchedEvent event) -> {
        if (event.getType() == NodeCreated) {
            LOG.info("WATCH triggered. Type {} for {}", event.getType(), event.getPath());
            getConDescription();
        }
    };

    /**
     * Gets the container description.
     */
    private void getConDescription() {
        zk.getData(conConfNode, false, getConDescriptionCallback, null);
    }

    /**
     * The object to call back with
     * {@link #getConDescription() getConDescription} method.
     */
    private final DataCallback getConDescriptionCallback = (int rc, String path, Object ctx, byte[] data, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                getConDescription();
                break;
            case NONODE:
                LOG.error("Node does NOT EXIST: " + path);
                break;
            case OK:
                LOG.info("Getting container description: " + path);
                // process container description
                processConDescription(data);

                executorService.execute(() -> {
                    // create conf file for container associated with the broker
                    createConfFile(container, container.getConSrvName());
                });

                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Initiates processing of the container description.
     *
     * @param data a serialized {@link Container Container}.
     */
    private void processConDescription(byte[] data) {
        // deserialize container 
        container = deserializeConType(data);
        /* initialize the services manager to manage services-dependencies
        The dependencies are retrieved from the current cotnainer configuration,
        from "requires" field.
         */
        List<String> srvNames = container.getRequires();
        Map<String, String> srvsNamePath = ns.getSrvsNamePath(srvNames);
        createServiceManager(srvsNamePath);
        lifecycleHandler.setSrvMngr(srvMngr);
        // set data to the container zNode 
        setZkConNodeData(data);
    }

    /**
     * Creates the {@link #srvMngr service manager}. Guarantees thread
     * visibility.
     *
     * @param srvsNamePath map with service name as key and service path as
     * value.
     */
    private void createServiceManager(Map<String, String> srvsNamePath) {
        synchronized (Broker.class) {
            srvMngr = new ServiceManager(srvsNamePath);
        }
    }

    /**
     * Sets data to the container's zNode.
     */
    private void setZkConNodeData(byte[] data) {
        zk.setData(zkContainerPath, data, -1, setConZkNodeDataCallback, data);
    }

    /**
     * Callback to be used with
     * {@link #setZkConNodeData(byte[]) setZkConNodeData} method.
     */
    private final StatCallback setConZkNodeDataCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                setZkConNodeData((byte[]) ctx);
                break;
            case NONODE:
                LOG.error("Cannot set data to znode. ZNODE DOES NOT EXIST: " + path);
                break;
            case OK:
                LOG.info("Data set to container zNode: " + path);
                // register container service to naming service
                registerToServices();
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * <p>
     * Registers the container as a service to the naming service.
     * <p>
     * The container name is also the name of the service. Every registered
     * service is represented by a service zNode. A service zNode is a
     * serialized {@link ZkNamingServiceNode ZkNamingServiceNode} object.
     * <p>
     * The service node contains the zNode path of the container offering the
     * service along with the status of the service (initialized or not).
     */
    private void registerToServices() {
        // create the service path for the naming service
        String path = ns.resolveSrvName(conSrvName);
        // set service status to NOT_INITIALIZED
        conZkSrvNode.setStatusNotInitialized();
        // serialize the node to byte array
        byte[] data = ns.serializeZkSrvNode(path, conZkSrvNode);
        // create the zNode of the service to the naming service
        createZkConSrvNode(path, data);
    }

    /**
     * Creates the service node for this container to the naming service.
     *
     * @param path the path of the zNode to the zookeeper namespace.
     * @param data the data of the zNode.
     */
    private void createZkConSrvNode(String path, byte[] data) {
        zk.create(path, data, OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL, createZkConSrvNodeCallback, data);
    }

    /**
     * The object to call back with {@link #createZkConSrvNode(java.lang.String, byte[])
     * createZkConSrvNode} method.
     */
    private final StringCallback createZkConSrvNodeCallback = (int rc, String path, Object ctx, String name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                checkContainerNode(path, (byte[]) ctx);
                break;
            case NODEEXISTS:
                LOG.error("Service zNode already exists: " + path);
                break;
            case OK:
                LOG.info("Registered to naming service: " + path);
                /* query for service - get the configurarion of needed containers
                A service is offered by a container. 
                 */
                if (srvMngr.hasServices()) {
                    srvMngr.getServices().stream().forEach((service) -> {
                        queryForService(service);
                    });
                } else {
                    executorService.execute(() -> {
                        lifecycleHandler.serviceNoneEvent();
                    });
                }
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Queries the naming service for a service. A service is offered by a
     * container. Every container offering a service registers to the naming
     * service. The name of the container is the name with which the container
     * registers itself to the naming service. The zNode of every service in the
     * naming service holds data. The data is the zNode path of the container to
     * the zookeeper namespace and the status of the service (initialized or
     * not).
     *
     * @param name the name of the service (the container name).
     */
    private void queryForService(String srvPath) {
        LOG.info("Querying for service: " + ns.resolveSrvPath(srvPath));
        // check if service has started
        serviceExists(srvPath);
    }

    /**
     * Checks if service exists.
     *
     * @param servicePath the path of the service under the naming service
     * namespace.
     */
    private void serviceExists(String servicePath) {
        zk.exists(servicePath, serviceWatcher, serviceExistsCallback, null);
    }

    /**
     * Callback to be used with
     * {@link  #serviceExists(java.lang.String) serviceExists} method.
     */
    private final StatCallback serviceExistsCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                serviceExists(path);
                break;
            case NONODE:
                LOG.warn("Service has NOT STARTED yet. Watch set to: " + path);
                break;
            case OK:
                LOG.info("Requested service found: " + path);
                // get data from service zNode.
                getZkSrvData(path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Watcher to be used with
     * {@link  #serviceExists(java.lang.String) serviceExists} method.
     */
    private final Watcher serviceWatcher = (WatchedEvent event) -> {
        LOG.info("WATCH triggered. Type {} for {}", event.getType(), event.getPath());
        if (event.getType() != null) {
            switch (event.getType()) {
                case NodeCreated:
                    // get data from service node
                    getZkSrvData(event.getPath());
                    break;
                case NodeDataChanged:
                    /* MONITOR FOR UPDATES ON THE SERVICE NODES - CHANGE OF STATE*/
                    getZkSrvUpdatedData(event.getPath());
                    break;
                case NodeDeleted:
                    /* ACTION TO TAKE IF SERVICE NODE IS REMOVED */
                    LOG.warn("A required service shutdown unexpectedly: {}", event.getPath());
                    srvMngr.deleteSrvNode(event.getPath());
                    lifecycleHandler.serviceDeletedEvent();
                    // re-set watch in case the service comes online
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Gets data from the requested service zNode.
     *
     * @param zkPath the path of the container to the zookeeper namespace.
     */
    private void getZkSrvData(String zkPath) {
        zk.getData(zkPath, serviceWatcher, getServiceDataCallback, null);
    }

    /**
     * The callback to be used with
     * {@link #getZkSrvData(java.lang.String) getZkSrvData(String)} method.
     */
    private final DataCallback getServiceDataCallback = (int rc, String path, Object ctx, byte[] data, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                getZkSrvData(path);
                break;
            case NONODE:
                LOG.error("CANNOT GET DATA from SERVICE. Service node DOES NOT EXIST: " + path);
                break;
            case OK:
                LOG.info("Getting data from service: " + path);
                // process retrieved data from requested service zNode
                processServiceData(data, path);
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
    private void processServiceData(byte[] data, String srvPath) {
        // de-serialize service node
        ZkNamingServiceNode node = ns.deserializeZkSrvNode(srvPath, data);
        // get the zNode path of the container of this service
        String zkConPath = node.getZkContainerPath();
        // store service info to the service manager 
        srvMngr.setSrvStateStatus(srvPath, node.getStatus());
        srvMngr.setSrvZkConPath(srvPath, zkConPath);
        // GET CONFIGURATION DATA FROM the container of the retrieved zkPath.
        getConData(zkConPath);
    }

    /**
     * Gets data from the container zNode.
     *
     * @param zkPath the path of the container zNode.
     */
    private void getConData(String zkPath) {
        zk.getData(zkPath, setConWatcher, getConDataDataCallback, null);
    }

    /**
     * The callback to be used with
     * {@link #getConData(java.lang.String) getConData(String)} method.
     */
    private final DataCallback getConDataDataCallback = (int rc, String path, Object ctx, byte[] data, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                getConData(path);
                break;
            case NONODE:
                LOG.error("CANNOT GET DATA from CONTAINER. Container node DOES NOT EXIST: " + path);
                break;
            case OK:
                LOG.info("Getting data from container: " + path);

                // process retrieved data from requested service zNode
                executorService.execute(() -> {
                    processConData(data, path);
                });

                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Processes data retrieved from container zNode.
     *
     * @param data the data of the container zNode.
     * @param zkConpath the path of the container zNode.
     */
    private void processConData(byte[] data, String zkConPath) {
        LOG.info("Processing container data: {}", zkConPath);
        // deserialize container data 
        Container srvCon = deserializeDependency(zkConPath, data);
        // get cotnainer name
        String conName = resolveConPath(zkConPath);
        // save configuration to file named after the container
        createConfFile(srvCon, conName);
        // save container to service node
        srvMngr.setSrvNodeCon(zkConPath, srvCon);
        // set the service conf status of service-dependency to PROCESSED
        srvMngr.setSrvConfStatusProc(ns.resolveSrvName(conName));
        // check if container is initialized in order to start processes
        lifecycleHandler.serviceAddedEvent();
    }

    /**
     * *************************************************************************
     * PROCESS HANDLING
     * *************************************************************************
     */
    /**
     * Creates the {@link ProcessManager ProcessManager}, creates the
     * environment for processes, initializes start-stops group processes,
     * executes tasks and start group processes, in that order.
     */
    @Override
    public void start() {
        LOG.info("Starting container processes initialization.");
        // create the process manager that will start processes
        createProcessManager();
        // create the environment for the container processes
        initProcsEnv();
        // initialization of process groups
        initProcGroups();
        // initialize handler for tasks
        initTaskHandler();
        // execute tasks
        taskHandler.execPreStartTasks();
        // execute START processes
        procMngr.exec_start_procs();
    }

    /**
     * Creates the {@link #procMngr process manager}. Guarantees thread
     * visibility.
     */
    private void createProcessManager() {
        synchronized (Broker.class) {
            procMngr = new ProcessManager();
        }
    }

    /**
     * Initializes process declared in start group and stop group.
     */
    private void initProcGroups() {
        // initialize processes in start group
        initStartGroup();
        // initialize processes in stop group
        initStopGroup();
    }

    /**
     * Initializes container processes defined in start group:
     * <ul>
     * <li>Initializes the {@link ProcessManager process manager} to manage
     * start group process execution.</li>
     * <li>Creates the {@link StartResMapper resource mapper}, that is
     * initialized with start resources, to handle resource manipulation.</li>
     * <li>Creates the environment for the processes (env vars).</li>
     * <li>Creates and initializes the {@link ProcessHandler process handlers}
     * for the main and other processes, to handle process initialization and
     * initiation.</li>
     * <li>Creates and initializes the
     * {@link StartGroupProcessHandler start handler} that handles the execution
     * of the processes defined in start group.</li>
     * </ul>
     */
    private void initStartGroup() {
        // create the resource mapper to map schema declarations to resources
        StartRes res = container.getStart();
        StartResMapper rm = new StartResMapper(res.getPreMain(), res.getPostMain(), res.getMain());
        // get the environment for the container processes
        Map<String, String> env = envHandler.getProcsEnv();
        // get handler for the interaction with the main process
        MainProcessHandler mainHandler = initMainProc(rm, env);
        // get handlers for the interaction with processes scheduled before main
        List<ProcessHandler> preMainHandlers = initDefaultProcs(rm, rm.getPreMainRes(), env);
        // get handlers for the interaction with processes scheduled after main
        List<ProcessHandler> postMainHandlers = initDefaultProcs(rm, rm.getPostMainRes(), env);
        // create and init start group process handler
        StartGroupProcessHandler startGroupHandler = new StartGroupProcessHandler(preMainHandlers, postMainHandlers, mainHandler);
        // code to execute on success
        startGroupHandler.setExecOnSuccess(() -> {
            // change service status to INITIALIZED
            updateZkSrvStatus(conZkSrvNode::setStatusInitialized);
            // monitor service in case it crashes
            monService();
        });
        // code to execute on failure
        boolean running = startGroupHandler.isMainProcRunning();
        startGroupHandler.setExecOnFailure(() -> {
            if (running) {
                // change service status to NOT_INITIALIZED
                updateZkSrvStatus(conZkSrvNode::setStatusNotInitialized);
            } else {
                // change service status to NOT_RUNNING
                updateZkSrvStatus(conZkSrvNode::setStatusNotRunning);
            }
        });
        // set configuration to process manager
        procMngr.setStartGroupHandler(startGroupHandler);
    }

    /**
     * Initializes all processes defined in stop group.
     */
    private void initStopGroup() {
        // get resources from stop section
        StopRes res = container.getStop();
        // create resource mapper to map resources from stop tag
        StopResMapper rm = new StopResMapper(res.getPreMain(), res.getPostMain(), res.getMain());
        // get the environment
        Map<String, String> env = null;
        if (envHandler != null) {
            env = envHandler.getProcsEnv();
        }
        // gather resources
        List<Resource> resources = new ArrayList<>();
        resources.addAll(rm.getPreMainRes());
        resources.add(rm.getMainRes());
        resources.addAll(rm.getPostMainRes());
        // init handlers
        List<ProcessHandler> handlers = initDefaultProcs(rm, resources, env);
        // init proc handler for stop group
        StopGroupProcessHandler stopGroupHandler = new StopGroupProcessHandler(handlers);
        procMngr.setStopGroupHandler(stopGroupHandler);
    }

    /**
     * Boots processes declared in stop group.
     */
    @Override
    public void stop() {
        // execute stop processes
        procMngr.exec_stop_procs();
        // execute tasks
        taskHandler.execPostStopTasks();
    }

    /**
     * <p>
     * Initializes the environment that will be used from processes. The
     * container environment is extracted along with the environment from its
     * dependencies.
     * <p>
     * The environment used by container processes consists of all the key-value
     * pairs of the environment variables declared in application schema.
     *
     * @return a map with all the key-value pairs of the environment variables
     * available to container processes.
     */
    private Map<String, String> initProcsEnv() {
        // get the environment obj of the container obj associated with Broker
        ContainerEnvironment conEnv = container.getEnv();
        // create map of container names and environment objs for dependencies
        Map<String, ContainerEnvironment> depConEnvMap = new HashMap<>();
        // get container objs from dependencies
        srvMngr.getConsOfSrvs().stream().forEach((con) -> {
            // get container name
            String name = con.getConSrvName();
            // get the environment obj according to the container type
            ContainerEnvironment env = con.getEnv();
            // add to map
            depConEnvMap.put(name, env);
        });
        // create environment mapper to map declared environments to env objects
        EnvironmentMapper envMap = new EnvironmentMapper(conEnv, conSrvName, depConEnvMap);
        // create handler to act on env objects
        envHandler = new EnvironmentHandler(envMap.getConEnv(), envMap.getDepConEnvMap());
        // create environment for processes
        return envHandler.createProcsEnv();
    }

    /**
     * <p>
     * Creates and initializes an executor for tasks, the {@link #taskHandler
     * taskHandler}. Guarantees thread visibility.
     * <p>
     * A task is a function of some type for the application.
     */
    private void initTaskHandler() {
        Tasks tasks = container.getTasks();
        Map<String, String> env;
        // if there are tasks defined
        if (tasks != null) {
            // create Task Mapper
            if (envHandler != null) {
                env = envHandler.getProcsEnv();
            } else {
                LOG.debug("Environment Handler NOT initialized");
                env = new HashMap<>();
            }
            TaskMapper tm = new TaskMapper(tasks, env);
            // init Task Handler
            synchronized (Broker.class) {
                taskHandler = new TaskHandler(tm.getPreStartTasks(), tm.getPostStopTasks());
            }
        } else {
            synchronized (Broker.class) {
                taskHandler = new TaskHandler();
            }
        }
    }

    /**
     * <p>
     * Sets all initialization configuration for the main process.
     * <p>
     * Creates and initializes the {@link MainProcessData MainProcessData}
     * object that stores all data related to the main process.
     * <p>
     * Creates and initializes the {@link MainProcessHandler MainProcessHandler}
     * object that handles the interaction with the main process.
     *
     * @param rh the resource mapper to query for resources to run.
     * @param env the environment of the process.
     * @return the {@link MainProcessHandler MainProcessHandler} object that
     * handles the interaction with the main process. NUll if there was a
     * problem with the main resource.
     */
    private MainProcessHandler initMainProc(StartResMapper rm, Map<String, String> env) {
        MainProcessHandler pHandler = null;
        // check if main resource is ok
        if (rm.isResourceOk(rm.getMainRes())) {
            // get the port the proc is listening
            int procPort = getHostPort();
            // create and init the object that stores all the process configuration
            MainProcessData pdata = new MainProcessData(rm.getMainRes(), env, "localhost", procPort);
            // create and init handler for main process execution
            pHandler = new MainProcessHandler(pdata);
        }
        return pHandler;
    }

    /**
     * <p>
     * Initializes any default process. A default process is a process that is
     * executed by a {@link DefaultProcessHandler DefaultProcessHandler}.
     * <p>
     * At first, a resource is checked for errors. Then the process handler is
     * initialized with the resource and the environment.
     *
     * @param rm the resource mapper, used to manipulate resources.
     * @param resources the list of the resources to execute.
     * @param env the environment of the processes.
     * @return the list of process handlers initialized to start processes.
     */
    private List<ProcessHandler> initDefaultProcs(ResourceMapper rm, List<Resource> resources, Map<String, String> env) {
        List<ProcessHandler> pHandlers = new ArrayList<>();
        // iterate through resources
        for (Resource res : resources) {
            // check resource for errors
            if (rm.isResourceOk(res)) {
                // create and init the obj that stores all necessary process data
                ProcessData pData = new ProcessData(res, env);
                // create and init the obj that handles the process execution
                DefaultProcessHandler ph = new DefaultProcessHandler(pData);
                // add handler to list
                pHandlers.add(ph);
            }
        }
        return pHandlers;
    }

    /**
     * <p>
     * Monitors the main process in case it stops abnormally and updates the
     * service status.
     * <p>
     * The method blocks.
     *
     * @param procHandler the {@link MainProcessHandler MainProcessHandler}
     * object.
     */
    private void monService() {
        // run in a new thread
        new Thread(() -> {
            procMngr.waitForMainProc();
            if (!Thread.interrupted()) {
                // change service status to NOT RUNNING if stopped for no reason
                if (!SHUTDOWN.isSignaledShutDown()) {
                    updateZkSrvStatus(conZkSrvNode::setStatusNotRunning);
                }
            }
        }
        ).start();
    }

    /**
     *
     * @return the port at which the main process runs.
     */
    private int getHostPort() {
        return container.getEnv().getHost_Port();
    }

    /**
     * Updates the service state status of a {@link ZkNamingServiceNode
     * ZkNamingServiceNode}.
     *
     * @param updateInterface the update action.
     */
    private void updateZkSrvStatus(Updatable updatableObj) {
        // get the service path
        String servicePath = ns.resolveSrvName(conSrvName);
        // update status
        updatableObj.updateStatus();
        LOG.info("Updating service status to {}: {}", conZkSrvNode.getStatus(), servicePath);
        // serialize data
        byte[] updatedData = ns.serializeZkSrvNode(servicePath, conZkSrvNode);
        // update service node data
        setZNodeData(servicePath, updatedData);
    }

    /**
     * Sets data to a zNode.
     */
    private void setZNodeData(String zNodePath, byte[] data) {
        zk.setData(zNodePath, data, -1, setZNodeDataDataCallback, data);
    }

    /**
     * Callback to be used with {@link #setZNodeData(java.lang.String, byte[])
     * setZNodeData} method.
     */
    private final StatCallback setZNodeDataDataCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                setZNodeData(path, (byte[]) ctx);
                break;
            case NONODE:
                LOG.error("Cannot set data to node. NODE DOES NOT EXITST: " + path);
                break;
            case OK:
                LOG.info("Data set to node: " + path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Watcher to be used in
     * {@link  #setConWatch(java.lang.String) setConWatch(String)} method.
     */
    private final Watcher setConWatcher = (WatchedEvent event) -> {
        LOG.info("WATCH triggered. Type {} for {}.", event.getType(), event.getPath());

        if (event.getType() == NodeDataChanged) {
            /**
             *
             * CODE FOR RETRIEVING UPDATES ON CONTAINER STATE REQUIRES
             * RECONFIGURING NOT IMPLEMENTED YET
             *
             */
            // RE-SET WATCH TO KEEP MONITORING THE CONTAINER
        }
    };

    /**
     * Gets data from the requested service zNode.
     *
     * @param zkPath the path of the container to the zookeeper namespace.
     */
    private void getZkSrvUpdatedData(String zkPath) {
        zk.getData(zkPath, serviceWatcher, getZkSrvUpdatedDataDataCallback, null);
    }

    /**
     * The callback to be used with
     * {@link #getZkSrvUpdatedData(java.lang.String) getZkSrvUpdatedData(String)}
     * method.
     */
    private final DataCallback getZkSrvUpdatedDataDataCallback = (int rc, String path, Object ctx, byte[] data, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                getZkSrvUpdatedData(path);
                break;
            case NONODE:
                LOG.error("CANNOT GET DATA from SERVICE. Service node DOES NOT EXIST: " + path);
                break;
            case OK:
                LOG.info("Getting data from service: " + path);
                // process retrieved data from requested service zNode
                executorService.execute(() -> {
                    processZkSrvUpdatedData(path, data);
                });
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    private void processZkSrvUpdatedData(String path, byte[] data) {
        // de-serialize service node
        ZkNamingServiceNode srvNode = ns.deserializeZkSrvNode(path, data);
        // set the new service status
        srvMngr.setSrvStateStatus(path, srvNode.getStatus());
        /*We received a status updated for a required service. The node had its data 
        changed. It was not created or deleted, because those actions are handled
        from different methods. Service status may have changed to INITIALIZED
        NOT_RUNNING, UPDATED*/

        if (srvNode.isStatusSetToUpdated()) {
            // service was updated so conf of service is reset to NOT_PROCESSED 
            srvMngr.setSrvConfStatusNotProc(path);
            /* RE-CONFIGURATION NEEDED!!! NOT IMPLEMENTED YET
            
            container node data has changed. Download again and do processing,
            just like getConData()->processConData(). At the end call
            lifecycleHandler.serviceUpdatedEvent instead of lifecycleHandler.serviceAddedEvent.
             */
        } else if (srvNode.isStatusSetToInitialized()) {
            lifecycleHandler.serviceInitializedEvent();
        } else if (srvNode.isStatusSetToNotRunning()) {
            lifecycleHandler.serviceNotRunnningEvent();
        } else if (srvNode.isStatusSetToNotInitialized()) {
            lifecycleHandler.serviceNotInitializedEvent();
        }
    }

    /**
     * Resolves a container path to the container name.
     *
     * @param path the zxNode container path to the zookeeper namespace.
     * @return the container name.
     */
    public final String resolveConPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }

    /**
     * Returns the container type.
     *
     * @param path the path of the cotnainer zNode to find the container type.
     * @return the container type.
     */
    protected final String getContainerType(String path) {
        return path.substring(path.indexOf("/", path.indexOf("/") + 1) + 1, path.lastIndexOf("/"));
    }

    /**
     * <p>
     * Creates a file with the container configuration.
     * <p>
     * Data to be written must be in json format.
     * <p>
     * The file is created to the BROKER_DIR directory. The full path of the
     * file is derived from the BROKER_DIR followed by the name of the
     * container.
     *
     * @param data the data to be written.
     * @param fileName the name of the file to hold the data.
     */
    private void createConfFile(Container con, String fileName) {
        // create the final file path
        String path = brokerConf.brokerDir + File.separator + fileName;
        // create new file
        File newFile = new File(path);
        // save data to file
        try {
            JAXBSerializer.saveToFile(newFile, con);
            // log event
            LOG.info("Created configuration file: {}", path);
        } catch (JAXBException | IOException ex) {
            LOG.error("FAILED to create configuration file: " + ex);
        }
    }

    /**
     * <p>
     * De-serializes the container associated with the broker.
     * <p>
     * The method is inherited by subclasses in order to implement custom
     * functionality according to the container type (web, business, data).
     *
     * @param data the data to be de-serialized.
     * @return a container object with the configuration.
     */
    protected abstract Container deserializeConType(byte[] data);

    /**
     * De-serialiazes a container.
     *
     * @param path the path of the container.
     * @param data the data to deserialize.
     * @return a {@link Container Container} object.
     */
    private Container deserializeDependency(String path, byte[] data) {
        Container con = null;
        // get the type of the container
        String type = getContainerType(path);
        // de-serialize the container according to the container type       
        try {
            if (type.equalsIgnoreCase("WebContainer")) {
                WebContainer webCon = JAXBSerializer.deserializeToWebContainer(data);
                con = webCon;
            } else if (type.equalsIgnoreCase("BusinessContainer")) {
                BusinessContainer businessCon = JAXBSerializer.deserializeToBusinessContainer(data);
                con = businessCon;
            } else if (type.equalsIgnoreCase("DataContainer")) {
                DataContainer dataCon = JAXBSerializer.deserializeToDataContainer(data);
                con = dataCon;
            }

            if (con != null) {
                LOG.info("De-serialized dependency: {}. Printing: \n {}", resolveConPath(path),
                        JAXBSerializer.deserializeToString(data));
            } else {
                LOG.error("De-serialization of dependency {} FAILED", path);
            }
        } catch (JAXBException ex) {
            LOG.error("De-serialization of dependency FAILED: " + ex);
        }

        return con;
    }

    /**
     * Serializes container configuration.
     *
     * @param conf the container configuration.
     * @return a byte array with the serialzed configuration.
     */
    private byte[] serializeConf(Container con) {
        byte[] data = null;
        try {
            data = JAXBSerializer.serialize(con);
            LOG.info("Configuration serialized SUCCESSFULLY!");
        } catch (JAXBException ex) {
            LOG.error("Serialization FAILED: " + ex);
        }
        return data;
    }

    /**
     * Sets a latch to wait for shutdown.
     */
    @Override
    public void waitForShutdown(ShutdownNotifier notifier) {
        try {
            notifier.waitForShutDown();
        } catch (InterruptedException ex) {
            // log the event
            LOG.warn("Interruption attemplted: {}", ex.getMessage());
            // set interrupted flag
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void shutdown(ShutdownNotifier notifier) {
        // signaled to shutdown 
        notifier.setSignaledShutDown(true);
        // excute stop commands if initialized
        if (procMngr != null) {
            if (procMngr.isStopHandlerInit()) {
                if (taskHandler != null) {
                    // run stop group procs
                    stop();
                } else {
                    initTaskHandler();
                    stop();
                }
            } else {
                initProcsEnv();
                initStopGroup();
                initTaskHandler();
                stop();
            }
        }
        // notify for shutdown objects implementing shutdown interface
        notifier.shutDown();
        // delete persistent zNode with container description to support restart
        deleteNode(conConfNode, -1);
        try {
            // close zk client session
            closeSession();
        } catch (InterruptedException ex) {
            // log the event
            LOG.warn("Thread Interruped. Stopping.");
            // set the interrupt status
            Thread.currentThread().interrupt();
        }
        // log event
        LOG.info("Initiating Broker shutdown " + zkContainerPath);
        // shut down the executorService to stop any still running threads
        shutdownExecutor();
    }

    @Override
    public void shutdown() {
        LOG.info("Starting container shutdown.");
        // wait services dependent on the service provided by this container
        waitDependentSrvsShutdown();
        // initiate container shutdown
        shutdown(SHUTDOWN);
    }

    private void shutdownExecutor() {
        executorService.shutdownNow();
    }

    /**
     * Waits for any services that depend on the service provides by this
     * container to finish shutdown and then initiates container shutdown.
     */
    private void waitDependentSrvsShutdown() {
        if (container != null) {
            if (!container.getIsRequiredFrom().isEmpty()) {
                LOG.info("Waiting dependent services shutdown.");
                int numOfDependentSrvs = container.getIsRequiredFrom().size();
                CountDownLatch dependentSrvShutdownSignal = new CountDownLatch(numOfDependentSrvs);
                execOnDependentSrvShutdown = () -> {
                    dependentSrvShutdownSignal.countDown();
                };
                // register watches for dependent services
                container.getIsRequiredFrom().stream().forEach((dependentSrv) -> {
                    setWatchOnDependentSrv(ns.resolveSrvName(dependentSrv));
                });
                // wait dependent services shutdown
                try {
                    dependentSrvShutdownSignal.await();
                } catch (InterruptedException ex) {
                    // log the event
                    LOG.warn("Thread Interruped. Stopping.");
                    // set the interrupt status
                    Thread.currentThread().interrupt();
                }
            } else {
                LOG.info("No dependent services.");
            }
        }
    }

    /**
     * Checks if service exists.
     *
     * @param servicePath the path of the service under the naming service
     * namespace.
     */
    private void setWatchOnDependentSrv(String servicePath) {
        zk.exists(servicePath, setWatchOnDependentSrvWatcher, setWatchOnDependentSrvCallback, null);
    }

    /**
     * Callback to be used with
     * {@link  #setWatchOnDependentSrv(java.lang.String) setWatchOnDependentSrv}
     * method.
     */
    private final StatCallback setWatchOnDependentSrvCallback = (int rc, String path, Object ctx, Stat stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOG.warn("Connection loss was detected. Retrying...");
                setWatchOnDependentSrv(path);
                break;
            case NONODE:
                LOG.warn("Dependent service does not exist: " + path);
                execOnDependentSrvShutdown.execute();
                break;
            case OK:
                LOG.info("Watch set for shutdown of dependent service: " + path);
                break;
            default:
                LOG.error("Something went wrong: ",
                        KeeperException.create(KeeperException.Code.get(rc), path));
        }
    };

    /**
     * Watcher to be used with
     * {@link  #setWatchOnDependentSrv(java.lang.String) setWatchOnDependentSrv}
     * method.
     */
    private final Watcher setWatchOnDependentSrvWatcher = (WatchedEvent event) -> {
        LOG.info("WATCH triggered. Type {} for {}", event.getType(), event.getPath());
        if (event.getType() != null) {
            switch (event.getType()) {
                case NodeDeleted:
                    LOG.info("Dependent service shutdown completed: {}", event.getPath());
                    execOnDependentSrvShutdown.execute();
                    break;
                case NodeDataChanged:
                    // re-set watch
                    setWatchOnDependentSrv(event.getPath());
                    break;
            }
        }
    };

    /**
     * Deletes the specified zNode. The zNode mustn't have any children. This
     * method uses the synchronous zk API.
     *
     * @param path the zNode to delete.
     * @param version the data version of the zNode.
     */
    private void deleteNode(String path, int version) {
        while (true) {
            try {
                zk.delete(path, version);
                LOG.info("Deleted node: {}", path);
                break;
            } catch (InterruptedException ex) {
                // log event
                LOG.warn("Interrupted. Stopping.");
                // set interupt flag
                Thread.currentThread().interrupt();
                break;
            } catch (KeeperException.ConnectionLossException ex) {
                LOG.warn("Connection loss was detected. Retrying...");
            } catch (KeeperException.NoNodeException ex) {
                LOG.info("Node already deleted: {}", path);
                break;
            } catch (KeeperException ex) {
                LOG.error("Something went wrong", ex);
                break;
            }
        }
    }

    @Override
    public void update() {
        LOG.info("Starting container re-configuration.");
    }

    @Override
    public void error() {
        LOG.error("Setting container into ERROR STATE.");
    }
}
