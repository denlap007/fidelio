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
package net.freelabs.fidelio.broker.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.freelabs.fidelio.broker.services.ServiceNode.SRV_CONF_STATUS;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.zookeeper.ZkNamingServiceNode.SRV_STATE_STATUS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * <p>
 * Class that holds all the information about the service-dependencies for the
 * container.
 * <p>
 * The container needs the configuration from the services in order to
 * initialize and also to ensure that the required services are initialized (not
 * just running) in order to start the main container process.
 */
public final class ServiceManager {

    /**
     * The required services by the container. The list holds the zNode service
     * path to the naming service namespace.
     */
    private final List<String> services = new ArrayList<>();
    /**
     * The service nodes of the required services.
     */
    private final Map<String, ServiceNode> srvNodes = new HashMap<>();
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);
    /**
     * Constructor.
     *
     * @param srvsNamePath a Map with K=service name, V=service znode path.
     */
    public ServiceManager(Map<String, String> srvsNamePath) {
        // create service nodes
        srvsNamePath.entrySet().stream().forEach((entry) -> {
            ServiceNode srvNode = new ServiceNode(entry.getKey(), entry.getValue(), "");
            // add to list with paths
            services.add(entry.getValue());
            // add to service nodes 
            srvNodes.put(entry.getValue(), srvNode);
        });
    }

    /**
     * Add a service node to the service manager.
     *
     * @param srvName the service name
     * @param srvPath the zNode path of the service to the naming service.
     * @param zkContPath the path of the container znode offering this service
     */
    public void createSrvNode(String srvName, String srvPath, String zkContPath) {
        ServiceNode srvNode = new ServiceNode(srvName, srvPath, zkContPath);
        srvNodes.put(srvPath, srvNode);
    }

    /**
     * Removes a service node from the service manager.
     *
     * @param srvPath the zNode path of the service to the naming service.
     */
    public synchronized void deleteSrvNode(String srvPath) {
        srvNodes.remove(srvPath);
    }

    public synchronized boolean areSrvProcessed() {
        StringBuilder waitingServices = new StringBuilder();

        srvNodes.entrySet().stream().map((entry) -> entry.getValue()).forEach((srvNode) -> {
            SRV_CONF_STATUS srvConfStatus = srvNode.getSrvConfStatus();
            if (srvConfStatus == SRV_CONF_STATUS.NOT_PROCESSED) {
                waitingServices.append(srvNode.getServiceName()).append(" ");
            }
        });

        if (waitingServices.toString().isEmpty()) {
            if (!srvNodes.isEmpty()) {
                LOG.info("All required services PROCESSED.");
            } else {
                LOG.info("No required services to process.");
            }
            return true;
        } else {
            LOG.info("Waiting to process services: {}", waitingServices);
            return false;
        }
    }

    public synchronized boolean areSrvInitialized() {
        StringBuilder waitingServices = new StringBuilder();

        srvNodes.entrySet().stream().map((entry) -> entry.getValue()).forEach((srvNode) -> {
            SRV_STATE_STATUS srvStateStatus = srvNode.getSrvStateStatus();
            if (srvStateStatus != SRV_STATE_STATUS.INITIALIZED
                    && srvStateStatus != SRV_STATE_STATUS.UPDATED) {
                waitingServices.append(srvNode.getServiceName()).append(" ");
            }
        });

        if (waitingServices.toString().isEmpty()) {
            if (!srvNodes.isEmpty()) {
                LOG.info("All required services INITIALIZED.");
            } else {
                LOG.info("No required services to wait for initialization.");
            }
            return true;
        } else {
            LOG.info("Waiting for services to initialize: {}", waitingServices);
            return false;
        }
    }

    /**
     * Sets the service configuration status to PROCESSED.
     *
     * @param srvPath the zNode path of the service to the naming service
     * namespace.
     */
    public synchronized void setSrvConfStatusProc(String srvPath) {
        ServiceNode srvNode = srvNodes.get(srvPath);
        srvNode.setSrvConfStatus(SRV_CONF_STATUS.PROCESSED);
        LOG.info("Configuration of service {} PROCESSED.", srvNode.getServiceName());
    }

    /**
     * Sets the service configuration status to NOT_PROCESSED.
     *
     * @param srvPath the zNode path of the service to the naming service
     * namespace.
     */
    public synchronized void setSrvConfStatusNotProc(String srvPath) {
        ServiceNode srvNode = srvNodes.get(srvPath);
        srvNode.setSrvConfStatus(SRV_CONF_STATUS.NOT_PROCESSED);
        LOG.info("Configuration of service {} NOT_PROCESSED.", srvNode.getServiceName());
    }

    /**
     * Sets the service state status.
     *
     * @param srvPath the zNode path of the service to the naming service
     * namespace.
     * @param newSrvStateStatus the updated service state status.
     */
    public synchronized void setSrvStateStatus(String srvPath, SRV_STATE_STATUS newSrvStateStatus) {
        ServiceNode srvNode = srvNodes.get(srvPath);
        srvNode.setSrvStateStatus(newSrvStateStatus);
        LOG.info("Status of service {} is {}.", srvNode.getServiceName(), newSrvStateStatus.toString());
    }

    /**
     * Sets the service state status to INITIALIZED.
     *
     * @param srvPath the zNode path of the service to the naming service
     * namespace.
     */
    public synchronized void setSrvStateStatusInit(String srvPath) {
        ServiceNode srvNode = srvNodes.get(srvPath);
        srvNode.setSrvStateStatus(SRV_STATE_STATUS.INITIALIZED);
        LOG.info("Status of service {} is {}.", srvNode.getServiceName(), SRV_STATE_STATUS.INITIALIZED.toString());
    }

    /**
     * Sets the service state status to NOT_RUNNING.
     *
     * @param srvPath the zNode path of the service to the naming service
     * namespace.
     */
    public synchronized void setSrvStateStatusNotRun(String srvPath) {
        ServiceNode srvNode = srvNodes.get(srvPath);
        srvNode.setSrvStateStatus(SRV_STATE_STATUS.NOT_RUNNING);
        LOG.info("Status of service {} is {}.", srvNode.getServiceName(), SRV_STATE_STATUS.NOT_RUNNING.toString());
    }

    /**
     * Sets the service state status to NOT_INITIALIZED.
     *
     * @param srvPath the zNode path of the service to the naming service
     * namespace.
     */
    public synchronized void setSrvStateStatusNotInit(String srvPath) {
        ServiceNode srvNode = srvNodes.get(srvPath);
        srvNode.setSrvStateStatus(SRV_STATE_STATUS.NOT_INITIALIZED);
        LOG.info("Status of service {} is {}.", srvNode.getServiceName(), SRV_STATE_STATUS.NOT_INITIALIZED.toString());
    }

    public void getSrvConfStatus(String srvPath) {
        srvNodes.get(srvPath).getSrvConfStatus();
    }

    public void setSrvNodeCon(String conPath, Container con) {
        for (ServiceNode srvNode : srvNodes.values()) {
            if (srvNode.getZkConPath().equals(conPath)) {
                srvNode.setCon(con);
            }
        }
    }

    public void setSrvZkConPath(String srvPath, String zkConPath) {
        srvNodes.get(srvPath).setZkConPath(zkConPath);
    }

    public synchronized boolean hasServices() {
        return !srvNodes.isEmpty();
    }

    public List<String> getServices() {
        return services;
    }

    /**
     *
     * @return a {@link List List} with the {@link Container Container} objects
     * of all services.
     */
    public List<Container> getConsOfSrvs() {
        List<Container> srvConList = new ArrayList<>();
        srvNodes.values().stream().forEach((srvNode) -> {
            srvConList.add(srvNode.getCon());
        });
        return srvConList;
    }
}
