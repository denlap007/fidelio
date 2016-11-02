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

/**
 *
 * Class that provides methods to access the configuration for the zookeeper
 * service.
 */
public final class ZkSrvConf {

    /**
     * The zookeeper client session time out.
     */
    private final int timeout;
    /**
     * The list of zookeepers hosts.
     */
    private final String hosts;

    /**
     * Constructor.
     *
     * @param hosts the list of zookeeper hosts. Must follow the format: 
     * <pre> HOST1_IP:PORT,HOST2_IP:PORT, ...</pre>
     *
     * @param sessionTimeout the time until session is closed if the client
     * hasn't contacted the server.
     */
    public ZkSrvConf(String hosts, int sessionTimeout) {
        this.timeout = sessionTimeout;
        this.hosts = hosts;
    }

    /**
     *
     * @return the list of zookeeper hosts.
     */
    public String getHosts() {
        return hosts;
    }

    /**
     *
     * @return the client session timeout.
     */
    public int getTimeout() {
        return timeout;
    }
}
