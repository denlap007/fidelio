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
package net.freelabs.fidelio.broker.boot;

import net.freelabs.fidelio.broker.Broker;
import net.freelabs.fidelio.broker.BusinessBroker;
import net.freelabs.fidelio.broker.DataBroker;
import net.freelabs.fidelio.broker.WebBroker;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Class that bootstraps the broker program.
 */
public class Bootstrap {

    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    // ---------------------------- THE MAIN -----------------------------------/
    /**
     * @param args args[0] -> zkHosts, args[1] -> zkSessionTimeout, args[2] ->
     * zkContainerPath, args[3] -> namingService, args[4] -> shutdownNode,
     * args[5] -> userConfNode
     */
    public static void main(String[] args) {
        Broker broker = null;
        String brokerThreadName = "";

        // Create and initialize broker according to container type
        if (args[2].contains("WebContainer")) {
            broker = new WebBroker(args[0], // zkHosts
                    Integer.parseInt(args[1]), // zkSessionTimeout
                    args[2], // zkContainerPath
                    args[3], // namingService
                    args[4], // shutdownNode
                    args[5] // userConfNode
            );
            // get the container name
            String name = broker.resolveConPath(args[2]);// zkContainerPath
            // set the thread name
            brokerThreadName = name + "-WebBrokerThraed";
        } else if (args[2].contains("BusinessContainer")) {
            broker = new BusinessBroker(args[0], // zkHosts
                    Integer.parseInt(args[1]), // zkSessionTimeout
                    args[2], // zkContainerPath
                    args[3], // namingService
                    args[4], // shutdownNode
                    args[5] // userConfNode
            );
            // get the container name
            String name = broker.resolveConPath(args[2]);// zkContainerPath
            // set the thread name
            brokerThreadName = name + "-BusinessBrokerThread";
        } else if (args[2].contains("DataContainer")) {
            broker = new DataBroker(args[0], // zkHosts
                    Integer.parseInt(args[1]), // zkSessionTimeout
                    args[2], // zkContainerPath
                    args[3], // namingService
                    args[4], // shutdownNode
                    args[5] // userConfNode
            );
            // get the container name
            String name = broker.resolveConPath(args[2]);// zkContainerPath
            // set the thread name
            brokerThreadName = name + "-DataBrokerThread";
        } else {
            LOG.error("No known container type found!");
        }

        if (broker != null) {
            // set name to thread
            Thread.currentThread().setName(brokerThreadName);
            // start broker
            broker.entrypoint();
        } else {
            LOG.error("FAILED to initialize broker!");
            errExit();
        }
    }

    /**
     * Exits with error code -1.
     */
    private static void errExit() {
        System.exit(-1);
    }
}
