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
package net.freelabs.fidelio.core.broker;

import com.github.dockerjava.api.DockerClient;
import net.freelabs.fidelio.core.schema.BusinessContainer;
import net.freelabs.fidelio.core.handlers.NetworkHandler;
import net.freelabs.fidelio.core.zookeeper.ZkConf;
import net.freelabs.fidelio.core.zookeeper.ZkMaster;

/**
 * Class that provides methods to handle initialization and bootstrapping of a
 * Business container type.
 */
public class BusinessBroker extends Broker {

    /**
     * The container description.
     */
    private final BusinessContainer con;

    /**
     * Constructor.
     *
     * @param zkConf the zookeeper configuration.
     * @param con the container object.
     * @param dockerClient an instance of a docker client.
     * @param master handles interaction with zookeeper service.
     * @param netHandler handles interaction with application networks.
     */
    public BusinessBroker(ZkConf zkConf, BusinessContainer con, DockerClient dockerClient, ZkMaster master, NetworkHandler netHandler) {
        super(zkConf, con, dockerClient, master, netHandler);
        this.con = con;
    }
}
