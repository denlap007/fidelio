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
package net.freelabs.fidelio.core.zookeeper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 *
 * Class that describes a node for the naming service.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ZkNamingServiceNode {

    /**
     * The zNode path of the container offering the service.
     */
    private String zkContainerPath;
    /**
     * The status of the service.
     */
    private SRV_STATE_STATUS status;

    /**
     * Defines the possible service state status values.
     */
    public static enum SRV_STATE_STATUS {
        INITIALIZED, NOT_INITIALIZED, NOT_RUNNING, UPDATED
    };

    /**
     * Constructor.
     *
     * @param zkContainerPath the zNode of the container offering the service.
     */
    public ZkNamingServiceNode(String zkContainerPath) {
        this.zkContainerPath = zkContainerPath;
        status = SRV_STATE_STATUS.NOT_RUNNING;
    }

    /**
     * Default constructor, necessary for de-serialization with Jackson/JAXB.
     */
    public ZkNamingServiceNode() {

    }

    /**
     *
     * @return the {@link #status status} of the service.
     */
    public SRV_STATE_STATUS getStatus() {
        return status;
    }

    /**
     * Sets node {@link #status status} to INITIALIZED.
     */
    public void setStatusInitialized() {
        synchronized (this) {
            status = SRV_STATE_STATUS.INITIALIZED;
        }
    }

    /**
     * Sets node {@link #status status} to NOT_INITIALIZED.
     */
    public void setStatusNotInitialized() {
        synchronized (this) {
            status = SRV_STATE_STATUS.NOT_INITIALIZED;
        }
    }

    /**
     * Sets node {@link #status status} to NOT_RUNNING.
     */
    public void setStatusNotRunning() {
        synchronized (this) {
            status = SRV_STATE_STATUS.NOT_RUNNING;
        }
    }

    /**
     * Sets node {@link #status status} to UPDATED.
     */
    public void setStatusUpdasted() {
        synchronized (this) {
            status = SRV_STATE_STATUS.UPDATED;
        }
    }

    public boolean isStatusSetToUpdated() {
        synchronized (this) {
            return status == SRV_STATE_STATUS.UPDATED;
        }
    }

    public boolean isStatusSetToInitialized() {
        synchronized (this) {
            return status == SRV_STATE_STATUS.INITIALIZED;
        }
    }

    public boolean isStatusSetToNotInitialized() {
        synchronized (this) {
            return status == SRV_STATE_STATUS.NOT_INITIALIZED;
        }
    }

    public boolean isStatusSetToNotRunning() {
        synchronized (this) {
            return status == SRV_STATE_STATUS.NOT_RUNNING;
        }
    }

    /**
     *
     * @return the zNode path of the container offering the service.
     */
    public String getZkContainerPath() {
        return zkContainerPath;
    }

}
