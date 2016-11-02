/*
 * Copyright (C) 2015-2016 Dionysis Lappas <dio@freelabs.net>
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
import javax.xml.bind.JAXBException;
import net.freelabs.fidelio.core.boot.ProgramConf;
import net.freelabs.fidelio.core.broker.BrokerInit;
import net.freelabs.fidelio.core.docker.DockerInitializer;
import net.freelabs.fidelio.core.handlers.ContainerHandler;
import net.freelabs.fidelio.core.serializer.JAXBSerializer;
import net.freelabs.fidelio.core.zookeeper.ZkConf;
import net.freelabs.fidelio.core.zookeeper.ZkMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that defines a command to restart a web application.
 */
public final class RestartCmd extends Command {

    /**
     * The master zookeeper process.
     */
    private ZkMaster master;
    /**
     * The zookeeper configuration for the deployed application.
     */
    private ZkConf zkConf;
    /**
     * The deployed application ID.
     */
    private String appID;
    /**
     * A docker client to query for state of containers.
     */
    private DockerClient docker;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RestartCmd.class);

    /**
     * Constructor.
     *
     * @param cmdName the name of the command.
     */
    public RestartCmd(String cmdName) {
        super(cmdName);
    }

    @Override
    protected void exec(ProgramConf pConf, String... args) {
        boolean success = false;
        // initialization
        init(pConf, args);
        // connect to zk
        master.connectToZk();
        // if no connection errors
        if (!master.isMasterError()) {
            // check if node with appID exists
            boolean exists = master.nodeExists(zkConf.getRoot().getPath());
            if (exists) {
                // download application conf
                boolean downloadedZkConf = downloadZkConf();
                // if conf was downloaded
                if (downloadedZkConf) {
                    // initialize docker client
                    initDockerClient(pConf.getDockerConf());
                    // create and initialize Broker initializer to act on containers
                    BrokerInit brokerInit = runBrokerInit();
                    // restart application
                    success = brokerInit.runRestart();
                    // check if operation succeeded
                    if (!success) {
                        // error occurred so stop any runnin services and containers
                        brokerInit.runStop();
                    }
                }
            } else {
                LOG.error("Application with id {} does NOT exist.", appID);
            }
        }

        master.shutdownMaster();

        if (success) {
            master.shutdownMaster();
            LOG.info("[Application Restarted] - id: {}", appID);
        } else {
            errExit();
        }
    }

    /**
     * Creates and initializes the {@link BrokerInit Broker Initializer} that
     * will handle interaction with containers.
     *
     * @return an initialized instance of {@link BrokerInit BrokerInit}.
     */
    private BrokerInit runBrokerInit() {
        // create container handler
        ContainerHandler handler = new ContainerHandler(zkConf.getWebApp().getContainers());
        // create and initialize the Broker Initializer
        return new BrokerInit(handler, zkConf, docker, master, null);
    }

    /**
     * Downloads node zkConf from zookeeper application tree and re-initializes
     * {@link #zkConf zkConf} with the configuration of the application as
     * deployed.
     *
     * @return true if zkConf node was successfully downloaded from zookeeper.
     */
    private boolean downloadZkConf() {
        LOG.info("Fetching application configuration...");
        boolean downloaded = false;
        byte[] data = master.nodeData(zkConf.getZkConf().getPath(), null);
        // check for errors
        if (data != null) {
            try {
                zkConf = JAXBSerializer.deserializeToZkConf(data);
                String dataStr = JAXBSerializer.deserializeToString(data);
                LOG.debug("Downloaded application configuration. Printing. {}", dataStr);
                downloaded = true;
            } catch (JAXBException ex) {
                LOG.error("Something went wrong: ", ex);
            }
        } else {
            LOG.error("Application data NOT found in zookeeper configuration node.");
        }
        return downloaded;
    }

    /**
     * Initializes necessary parameters.
     *
     * @param pConf program's configuration.
     * @param args arguments defined in command line.
     */
    private void init(ProgramConf pConf, String... args) {
        // the application to restart
        appID = args[0];
        // initialize object to re-create application namespace
        zkConf = new ZkConf(appID, pConf.getZkHosts(), pConf.getZkSessionTimeout());
        // initialize master to connect to zookeeper
        master = new ZkMaster(zkConf);
    }

    /**
     * Initializes a docker client.
     *
     * @param dockerURI the uri of the docker host.
     */
    private void initDockerClient(String[] dockerConf) {
        // create a docker client 
        DockerInitializer appDocker = new DockerInitializer(dockerConf);
        docker = appDocker.getDockerClient();
    }

    /**
     * Exits program with error code (1), exit due to error.
     */
    @Override
    protected void errExit() {
        LOG.error("Restart of {} FAILED. Exiting...", appID);
        System.exit(1);
    }

}
