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
package net.freelabs.fidelio.core.cmd;

import com.github.dockerjava.api.DockerClient;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import net.freelabs.fidelio.core.broker.Broker;
import net.freelabs.fidelio.core.analyzers.Analyzer;
import net.freelabs.fidelio.core.boot.ProgramConf;
import net.freelabs.fidelio.core.broker.BrokerInit;
import net.freelabs.fidelio.core.xml.XmlProcessor;
import net.freelabs.fidelio.core.docker.DockerInitializer;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.schema.WebApp;
import net.freelabs.fidelio.core.handlers.ContainerHandler;
import net.freelabs.fidelio.core.handlers.NetworkHandler;
import net.freelabs.fidelio.core.serializer.JAXBSerializer;
import net.freelabs.fidelio.core.utils.Utils;
import net.freelabs.fidelio.core.zookeeper.ZkConf;
import net.freelabs.fidelio.core.zookeeper.ZkMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that starts and configures program's components.
 */
public final class StartCmd extends Command {

    /**
     * The master zk process.
     */
    private ZkMaster master;
    /**
     * The thread running the master process
     */
    private Thread masterThread;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StartCmd.class);

    /**
     * Constructor.
     *
     * @param cmd the name of the command.
     */
    public StartCmd(String cmd) {
        super(cmd);
    }

    @Override
    public void exec(ProgramConf pConf, String... args) {
        try {
            // unmarshall xml file into a top-level object
            WebApp webApp = unmarshalXml(pConf.getXmlSchemaPath(), pConf.getXmlFilePath());
            if (webApp == null){
                errExit();
            }
            // create a handler to query for container information
            ContainerHandler handler = createConHandler(webApp);
            // analyze restrictions and check if apply on schema
            analyze(handler);
            // create zk configuration
            ZkConf zkConf = createZkConf(webApp, pConf.getZkHosts(), pConf.getZkSessionTimeout(), handler, pConf);
            // initialize zk and start master process
            initZk(zkConf);
            // create a docker client customized for the app
            DockerInitializer dockerInit = new DockerInitializer(pConf.getDockerConf());
            DockerClient docker = dockerInit.getDockerClient();
            // creaet application network handler
            NetworkHandler netHandler = new NetworkHandler(docker);
            // create network for application
            boolean netCreated = netHandler.createNetwork(zkConf.getAppDefaultNetName());
            if (netCreated) {
                // launch the CoreBrokers to boot containers, wait to finish
                runBrokerInit(handler, zkConf, docker, netHandler);
            } else {
                //cleanup
                master.cleanZkNamespace();
                errExit();
            }
        } catch (Exception ex) {
            exitProgram(ex);
        }
    }

    /**
     * <p>
     * Initiates master process shutdown.
     * <p>
     * The method blocks.
     */
    private void shutdownMaster() {
        // shutdown 
        master.shutdownMaster();
        try {
            // wait to finish
            masterThread.join();
        } catch (InterruptedException ex) {
            LOG.warn("Thread interrupted. Stopping.");
            // set the interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * <p>
     * Analyzes the restrictions that must apply on the schema and applies any 
     * necessary processing.
     * <p>
     * No circular dependencies are allowed.
     * <p>
     * No duplicate container names are allowed.
     * <p>
     * Populates {@link Container#isRequiredFrom isRequiredFrom} list for every 
     * container.
     *
     * @param handler object to query for containers.
     */
    private void analyze(ContainerHandler handler) {
        // create analyzer to check restrictions on schema
        Analyzer analyzer = new Analyzer(handler.listContainers());

        // analyze dependencies
        LOG.info("Checking service dependencies...");
        // search for circular dependencies
        boolean found = analyzer.detectCircularDependencies();
        // if circular dependencies found exit
        if (found) {
            errExit();
        }

        // analyze container names 
        LOG.info("Checking service names...");
        found = analyzer.detectDuplicateNames();
        // if duplicate contianer names found exit
        if (found) {
            errExit();
        }
        
        // populate isRequiredFrom lists
        analyzer.populateIsRequiredFromLists();
    }

    /**
     * <p>
     * Launches the {@link Broker Brokers} that boot the containers.
     * <p>
     * A {@link Container Container} is obtained from the {@link ContainerHandler
     * ContainerHandler}. A {@link Broker Broker} is created and then connects
     * to zk and starts execution a new thread.
     * <p>
     * The method gets all {@link Container Containers} exhaustively and starts
     * all {@link Broker CoreBrokers} necessary for the program.
     *
     * @param handler object to query for containers.
     * @param zkConf the zk configuration.
     * @param docker a docker client.
     * @param netHandler handles interaction with application networks.
     * @throws IOException if connection to zk cannot be established.
     * @throws InterruptedException if thread is interrupted.
     */
    public void runBrokerInit(ContainerHandler handler, ZkConf zkConf, DockerClient docker, NetworkHandler netHandler) throws IOException, InterruptedException {
        /*  Get a Container from the container handler. The Container can be of 
            any type. Create the Broker and initialize it. The Broker will 
            connect to zk and then start execution on a new thread.
         */
        BrokerInit brokerInit = new BrokerInit(handler, zkConf, docker, master, netHandler);
        // run the Broker initializer that will initialize start and execute Brokers
        boolean success = brokerInit.runStart();
        // check if operation was successful 
        if (!success) {
            LOG.info("Performing cleanup...");
            brokerInit.cleanupFromFailedStart();
            // remove default network
            netHandler.deleteNetwork(zkConf.getAppDefaultNetName());
            // delete application namespace
            master.cleanZkNamespace();
            // shutdown master
            shutdownMaster();
            errExit();
        } else {
            // shutdown master
            shutdownMaster();
            LOG.info("[Application Deployed] - id: {}", master.getDeployedID());
        }
    }

    /**
     * Unmarshals an xml file.
     *
     * @param schemaPath the path to the xml schema.
     * @param xmlFilePath the path to the xml file.
     * @return an object of the top-level element.
     */
    public WebApp unmarshalXml(String schemaPath, String xmlFilePath) {
        // create an object that processes initial configuration
        XmlProcessor proc = new XmlProcessor();
        // define the name of the package that contains the classes for binding
        String packageName = "net.freelabs.fidelio.core.schema";
        // Get root Object 
        return (WebApp) proc.unmarshal(packageName, schemaPath, xmlFilePath);
    }

    /**
     * <p>
     * Returns a container handler object.
     * <p>
     * A container handler is queried from other objects to obtain information
     * about the containers and/or the containers themselves.
     *
     * @param webApp an object of class WebApp. This object represents the
     * top-level element from an unmarshalled xml file.
     * @return a container handler.
     */
    public ContainerHandler createConHandler(WebApp webApp) {
        // Get a handler for containers
        return (new ContainerHandler(webApp.getContainers()));

    }

    /**
     * <p>
     * Creates all the necessary zookeeper configuration for the program to
     * start.
     * <p>
     * The Zookeeper configuration is stored at a
     * {@link net.freelabs.fidelio.zookeeper.ZkConfig ZkConf} object. The object
     * stores zk connection configuration along with the zk namespace nodes that
     * need to be created for the application.
     * <p>
     * A handler is passed to retrieve information from/and containers in order
     * to initialize the configuration. The Container Types are the parent nodes
     * of the zk namespace and Containers are the child nodes.
     *
     * @param webApp the application description.
     * @param hosts the host:port zookeeper server list.
     * @param timeout the client session timeout.
     * @param handler a Container handler object to query for container
     * information.
     * @param pConf the program's configuration.
     * @return a {@link net.freelabs.fidelio.zookeeper.ZkConfig ZkConf} object
     * that holds all the configuration for zookeeper.
     * @throws javax.xml.bind.JAXBException if serialization of Container object
     * fails.
     */
    public ZkConf createZkConf(WebApp webApp, String hosts, int timeout, ContainerHandler handler, ProgramConf pConf) throws JAXBException {
        LOG.info("Creating application configuration for zookeeper...");
        /*
         Create a zookeeper configuration object. This object holds all the
         necessary configuration information needed for zookeeper to boostrap-
         initialize and for the program to work. Among others, it holds the host:port 
         connection server list, client session timeout, paths and data of parent 
         zookeeper nodes that correspond to the first nodes created -based to the 
         different Container Types (e.g. web, business, data e.t.c.) declared, 
         paths and data of children zookeeper nodes that correspond to the children 
         of parent nodes based on declared Containers e.t.c.
         */
        ZkConf zkConf = new ZkConf("", hosts, timeout);

        // save application description
        zkConf.setWebApp(webApp);

        /* 
         Initialize zookeeper parent nodes. The zookeeper namespace has an 
         hierarchical structure of nodes like a file system, called zNodes. ZNodes
         are referenced by paths and may store data.  
         The parent nodes are created on the root of the zookeeper namespace. Each
         parent node's path is construted using a Container Type (e.g. /web). The 
         parent nodes usually have children. The children correspond to Containers.    
        
         We query the Container Handler for the available Container Types and then
         we initialize the zookeeper parent nodes. We store no data to these nodes.
         Parent nodes are Persistent zNodes.
         */
        for (String type : handler.getContainerTypes()) {
            LOG.debug("Initializing container type: {}", type);
            zkConf.initZkContainerType(type);
        }

        /*
         Initialize zookeeper child nodes. Every child node (Container) belongs to
         a parent node (Container Type). Child nodes are Ephemeral zNodes and cannot 
         have children of their own.
        
         We query the Container Handler for the available Containers and then we
         initialize the zookeeper child nodes. The data of a child node is a 
         serialized Container object.
         */
        for (Container con : handler.listContainers()) {
            // generate JSON from container and return the generated JSON as a byte array
            byte[] data = JAXBSerializer.serialize(con);
            LOG.debug("Serialized container description of service {}: {}", con.getConSrvName(), JAXBSerializer.deserializeToString(data));
            // get the name for the child node
            String name = con.getConSrvName();
            // get the container type
            String type = Utils.getType(con);
            // initialize child node
            LOG.debug("Initializing zkConf node for service {} of type {} with data {} bytes", con.getConSrvName(), type, data.length);
            zkConf.initZkContainer(name, type, data);
        }

        // store program configuration 
        zkConf.setpConf(pConf);

        // initialize  deployed container names
        zkConf.initDeplCons(handler.listContainerNames());

        // initialize zkConf node with data
        byte[] data = JAXBSerializer.serialize(zkConf);
        zkConf.getZkConf().setData(data);
        // LOG.debug("Serialized zkConf: {}", JAXBSerializer.deserializeToString(data));
        return zkConf;
    }

    /**
     * <p>
     * Starts a {@link net.freelabs.fidelio.core.zookeeper.ZkMaster Master}
     * process that connects to the zookeeper servers, initializes the zookeeper
     * namespace, registers NodeCreated watch events for new zNodes created by a
     * {@link net.freelabs.fidelio.broker.Broker Broker}.
     * <p>
     * This method starts a Master process to initialize zk. The master
     * establishes a session with the zookeeper servers. After the session is
     * established, the Master is created, the watched event is processed and
     * bootstraps the creation of the namespace from the
     * {@link net.freelabs.fidelio.zookeeper.ZkConfig ZkConf} object.
     *
     * @param zkConf an object with the zk configuration.
     * @throws InterruptedException if thread is interrupted.
     * @throws IOException in cases of network failure.
     */
    public void initZk(ZkConf zkConf) throws InterruptedException, IOException {
        // create a master object and initialize it with zk configuration
        master = new ZkMaster(zkConf);
        // Create a new thread to run the master 
        masterThread = new Thread(master, "Master-thread");
        // start the master process
        masterThread.start();
        // wait for initialization
        LOG.debug("WAITING FOR MASTER INITIALIZATION");
        // check initialization
        if (!master.isMasterInitialized()) {
            errExit();
        }
    }

    /**
     * Terminates the program due to some error printing the cause.
     */
    private void exitProgram(Exception ex) {
        LOG.error("Something went wrong: {}", ex.getMessage());
        if (master != null) {
            master.shutdown();
        }
        errExit();
    }

    /**
     * Terminates the program due to some error.
     */
    @Override
    protected void errExit() {
        // log 
        LOG.error("FAILED to deploy application.");
        System.exit(-1);
    }
}
