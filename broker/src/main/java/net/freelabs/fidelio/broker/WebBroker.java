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
import net.freelabs.fidelio.core.schema.WebContainer;
import net.freelabs.fidelio.core.serializer.JAXBSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dionysis Lappas <dio@freelabs.net>
 */
public class WebBroker extends Broker {

    private WebContainer conObj;

    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Broker.class);

    public WebBroker(String zkHosts, int zkSessionTimeout, String zkContainerPath, String zkNamingService, String shutdownNode, String userConfNode) {
        super(zkHosts, zkSessionTimeout, zkContainerPath, zkNamingService, shutdownNode, userConfNode);
    }

    @Override
    public Container deserializeConType(byte[] data) {
        WebContainer con = null;
        try {
            con = JAXBSerializer.deserializeToWebContainer(data);
            LOG.info("Configuration deserialized! Printing: \n {}",
                    JAXBSerializer.deserializeToString(data));
        } catch (JAXBException ex) {
            LOG.error("De-serialization FAILED: " + ex);
        }
        // initialize instance
        conObj = con;
        return con;
    }

}
