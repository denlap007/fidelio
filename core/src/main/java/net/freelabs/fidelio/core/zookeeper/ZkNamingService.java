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
package net.freelabs.fidelio.core.zookeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import net.freelabs.fidelio.core.serializer.JAXBSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that provides methods to facilitate the interaction with the the
 * zookeeper naming service for the application.
 */
public class ZkNamingService {

    /**
     * The zNode path of the naming service node in the zookeeper namespace.
     */
    private final String zkNamingServicePath;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ZkNamingService.class);

    /**
     * Constructor
     *
     * @param zkNamingServicePath
     */
    public ZkNamingService(String zkNamingServicePath) {
        // store the name of the naming service node
        this.zkNamingServicePath = zkNamingServicePath;
    }

    /**
     * <p>
     * Resolves a service name to the service path.
     * <p>
     * A service name is a container name. Every running container, after
     * initialization, registers itself to the naming service as a service. The
     * path of the zNode created by this service under the naming service zNode
     * is the service path.
     *
     * @param service the service name.
     * @return the service path to the zookeeper namespace.
     */
    public final String resolveSrvName(String service) {
        return zkNamingServicePath.concat("/").concat(service);
    }

    /**
     * Resolves a service path to the service name.
     *
     * @param path the service path to the zookeeper namespace.
     * @return the service name.
     */
    public final String resolveSrvPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }

    /**
     * Serializes a {@link ZkNamingServiceNode ZkNamingServiceNode}.
     *
     * @param path the zNode path of the service.
     * @param node the service node to serialize.
     * @return a byte array representing the serialized node.
     */
    public byte[] serializeZkSrvNode(String path, ZkNamingServiceNode node) {
        byte[] data = null;
        try {
            data = JAXBSerializer.serialize(node);
            LOG.info("Serialized service node: {}", path);
        } catch (JAXBException ex) {
            LOG.error("Service node Serialization FAILED: " + ex);
        }
        return data;
    }

    /**
     * De-serializes a {@link ZkNamingServiceNode ZkNamingServiceNode} that is
     * stored as a byte array.
     *
     * @param path the zNode path of the service.
     * @param data the data to de-serialize.
     * @return a {@link ZkNamingServiceNode ZkNamingServiceNode} with the data
     * of the specified service node.
     */
    public ZkNamingServiceNode deserializeZkSrvNode(String path, byte[] data) {
        ZkNamingServiceNode node = null;
        try {
            node = JAXBSerializer.deserializeToServiceNode(data);
            LOG.info("De-serialized service node: {}", path);
        } catch (JAXBException ex) {
            LOG.error("Service node de-serialization FAILED! " + ex);
        }
        return node;
    }

    /**
     * <p>
     * Resolves a list of service names to the service paths.
     *
     * @param srvNames the service names to resolve.
     * @return a list with the service paths.
     */
    public List<String> resolveServicePaths(List<String> srvNames) {
        List<String> srvPaths = new ArrayList<>();
        srvNames.stream().forEach((srvName) -> {
            srvPaths.add(resolveSrvName(srvName));
        });
        return srvPaths;
    }
    
    /**
     * 
     * @param srvNames the service names.
     * @return A Map with the service names as key and service paths as value.
     */
    public Map<String, String> getSrvsNamePath(List<String> srvNames){
        Map<String, String> srvsNamePath = new HashMap<>();
        
        srvNames.stream().forEach((srvName) -> {
            srvsNamePath.put(srvName, resolveSrvName(srvName));
        });
        
        return srvsNamePath;
    }

}
