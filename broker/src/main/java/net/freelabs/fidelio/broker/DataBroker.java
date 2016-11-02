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
package net.freelabs.fidelio.broker;

import javax.xml.bind.JAXBException;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.schema.DataContainer;
import net.freelabs.fidelio.core.serializer.JAXBSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dionysis Lappas <dio@freelabs.net>
 */
public class DataBroker extends Broker {

    private DataContainer conObj;

    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataBroker.class);

    /**
     * Constructor.
     *
     * @param zkHosts the zookeeper hosts list.
     * @param zkSessionTimeout the client session timeout.
     * @param zkContainerPath the path of the Container to the zookeeper
     * namespace.
     * @param zkNamingService the path of the naming service to the zookeeper
     * namespace.
     * @param shutdownNode the node the signals the shutdown.
     * @param userConfNode the node with the initial container configuration.
     * @param conDataNode the node with data uploaded to be used by the container.
     */
    public DataBroker(String zkHosts, int zkSessionTimeout, String zkContainerPath, String zkNamingService, String shutdownNode, String userConfNode) {
        super(zkHosts, zkSessionTimeout, zkContainerPath, zkNamingService, shutdownNode, userConfNode);
    }

    @Override
    public Container deserializeConType(byte[] data) {
        DataContainer con = null;
        try {
            con = JAXBSerializer.deserializeToDataContainer(data);
            LOG.info("Configuration deserialized! Printing: \n {}",
                    JAXBSerializer.deserializeToString(data));
        } catch (JAXBException ex) {
            LOG.error("De-serialization FAILED: " + ex);
        }
        // initialize instance var
        conObj = con;

        return con;
    }

}
