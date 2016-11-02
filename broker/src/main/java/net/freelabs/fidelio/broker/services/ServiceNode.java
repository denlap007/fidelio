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

import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.zookeeper.ZkNamingServiceNode.SRV_STATE_STATUS;

/**
 *
 * Class that holds all the information about a service required by the container.
 */
public class ServiceNode {
    /**
     * The name of the service. Also the name of the container with that service.
     */
    private String srvName;
    /**
     * The zNode path of the service to the naming service namespace.
     */
    private String srvPath;
    /**
     * The zNode path of the container offering the service.
     */
    private String zkConPath;
    /**
     * The configuration of the container offering the service.
     */
    private Container con;

    /**
     * Defines the possible service configuration status values.
     */
    public static enum SRV_CONF_STATUS {
        PROCESSED, NOT_PROCESSED
    };
    /**
     * The service configuration status.
     */
    private SRV_CONF_STATUS srvConfStatus;

    /**
     * The service state status.
     */
    private SRV_STATE_STATUS srvStateStatus;  

    /**
     * Constructor.
     *
     * @param srvName the name of the service.
     * @param srvPath the zNode path of the service under the naming service 
     * namepsace.
     * @param zkConPath the znode path of the container offering the service.
     */
    public ServiceNode(String srvName, String srvPath, String zkConPath) {
        this.srvName = srvName;
        this.srvPath = srvPath;
        this.zkConPath = zkConPath;
        // set service state status
        this.srvStateStatus = SRV_STATE_STATUS.NOT_RUNNING;
        // set configuration status 
        srvConfStatus = SRV_CONF_STATUS.NOT_PROCESSED;
    }

    /**
     * Default constructor, NECESSARY for de-serialization with Jackson.
     */
    public ServiceNode() {

    }

    // Getters -Setters
    /**
     * 
     * @return the service name.
     */
    public String getServiceName() {
        return srvName;
    }
    /**
     * 
     * @param srvName the service name.
     */
    public void setServiceName(String srvName) {
        this.srvName = srvName;
    }
    /**
     * 
     * @return the zNode path of the service to the naming service 
     * namespace.
     */
    public String getServicePath() {
        return srvPath;
    }
    /**
     * 
     * @param srvPath the zNode path of the service to the naming service 
     * namespace.
     */
    public void setServicePath(String srvPath) {
        this.srvPath = srvPath;
    }
    /**
     * 
     * @return the zNode path of the container offering that service.
     */
    public String getZkConPath() {
        return zkConPath;
    }
    /**
     * 
     * @param zkConPath the zNode path of the container offering that service.
     */
    public void setZkConPath(String zkConPath) {
        this.zkConPath = zkConPath;
    }
    /**
     * 
     * @return the configuration of the container offering the service.
     */
    public Container getCon() {
        return con;
    }
    /**
     * 
     * @param con the configuration of the container offering the service.
     */
    public void setCon(Container con) {
        this.con = con;
    }
    /**
     * 
     * @return the service configuration status.
     */
    public SRV_CONF_STATUS getSrvConfStatus() {
        return srvConfStatus;
    }
    /**
     * 
     * @param srvConfStatus the service configuration status.
     */
    public void setSrvConfStatus(SRV_CONF_STATUS srvConfStatus) {
        this.srvConfStatus = srvConfStatus;
    }
    /**
     * 
     * @return the service state status.
     */
    public SRV_STATE_STATUS getSrvStateStatus() {
        return srvStateStatus;
    }
    /**
     * 
     * @param srvStateStatus the service state status.
     */
    public void setSrvStateStatus(SRV_STATE_STATUS srvStateStatus) {
        this.srvStateStatus = srvStateStatus;
    }
}
