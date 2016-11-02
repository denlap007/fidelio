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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import net.freelabs.fidelio.core.boot.ProgramConf;
import net.freelabs.fidelio.core.schema.WebApp;

/**
 *
 * Class that provides methods to access configuration information of a web
 * application regarding its interaction with zookeeper service.
 * <p>
 * The zookeeper namespace of the app is defined along with upData for the
 * nodes.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@XmlAccessorType(XmlAccessType.FIELD)
public final class ZkConf {

    /**
     * The root zkNode for the application.
     */
    private ZkNode root;
    /**
     * The naming service zkNode for the application.
     */
    private ZkNode services;
    /**
     * The container description zkNode for the application. Under this node
     * will be saved all container descriptions.
     */
    private ZkNode conDesc;
    /**
     * The shutdown node for the application. When created indicates program
     * shutdown.
     */
    private ZkNode shutdown;
    /**
     * The zkNode with all the configuration regarding zookeeper service and
     * application deployment to zookeeper service.
     */
    private ZkNode zkConf;
    /**
     * The application description.
     */
    private WebApp webApp;
    /**
     * The zkNodes holding info about the container types.
     */
    private List<ZkNode> containerTypes;
    /**
     * The zkNodes holding info about containers.
     */
    private Map<String, ZkNode> containers;
    /**
     * Map of zkNodes paths holding upload/download upData about containers. The
     * map key is the name of the container and the value is the zpath to the
     * upData node.
     */
    private Map<String, String> conUpDataNodes;
    /**
     * The namespace of the application to the zookeeper service.
     */
    @JsonIgnore
    @XmlTransient
    private List<ZkNode> zkAppNamespace;
    /**
     * The client configuration that connects to the zookeeper service.
     */
    @JsonIgnore
    @XmlTransient
    private ZkSrvConf zkSrvConf;
    /**
     * Map of the defined container names as key and the deployed container
     * names as value.
     */
    private Map<String, String> deplCons;
    /**
     * The program's configuration
     */
    @XmlTransient
    private ProgramConf pConf;
    /**
     * The name of the default network for the application.
     */
    private String appDefaultNetName;
    /**
     * An id used as upData for nodes without upData. Also, this is the suffix
     * to the zk root node for the application.
     */
    private String suffix;
    /**
     * The prefix of the root zookeeper node name for the application namespace
     * hierarchy.
     */
    private static final String ROOT_PREFIX = "fidelio-";

    /**
     * Constructor.
     *
     * @param root the name of the root node for the application namespace in
     * zookeeper.
     * @param hosts the list of zookeeper hosts. Must follow the format: 
     * <pre> HOST1_IP:PORT,HOST2_IP:PORT, ...</pre>
     *
     * @param timeout the time until session is closed if the client hasn't
     * contacted the server.
     */
    public ZkConf(String root,
            String hosts,
            int timeout) {
        // create namespace list and uploadNodes map
        zkAppNamespace = new ArrayList<>();
        conUpDataNodes = new HashMap<>();
        // create root zkNode
        String name;
        if (root.isEmpty()) {
            // create ID for root node, a 10-digit positive zero padded number
            suffix = generateId();
            name = ROOT_PREFIX + suffix;
        } else {
            name = root;
            String[] tokens = name.split("-");
            if (tokens != null) {
                if (tokens.length == 2) {
                    suffix = tokens[1];
                } else {
                    suffix = "0";
                }
            } else {
                suffix = "0";
            }
        }
        String rootPath = "/" + name;
        this.root = new ZkNode(rootPath, suffix.getBytes(), name, "");
        zkAppNamespace.add(this.root);
        // create services zkNode
        String path = rootPath + "/services";
        name = "services";
        services = new ZkNode(path, suffix.getBytes(), name, "");
        zkAppNamespace.add(services);
        // create zknode for container descriptions
        path = rootPath + "/conf";
        name = "conf";
        conDesc = new ZkNode(path, suffix.getBytes(), name, "");
        zkAppNamespace.add(conDesc);
        // create shutdown zkNode
        path = rootPath + "/shutdown";
        name = "shutdown";
        shutdown = new ZkNode(path, suffix.getBytes(), name, "");
        // create zkNode for zookeeper configuration
        path = rootPath + "/zkConf";
        name = "zkConf";
        zkConf = new ZkNode(path, suffix.getBytes(), name, "");
        zkAppNamespace.add(zkConf);
        // create Lists-Maps
        containerTypes = new ArrayList<>();
        containers = new HashMap<>();
        deplCons = new HashMap<>();
        // initialize client configuration
        zkSrvConf = new ZkSrvConf(hosts, timeout);
        // set default app network name
        appDefaultNetName = this.root.getName() + "-net";
    }

    /**
     * Default Constructor FOR JACKSON /JAXB COMPATIBILITY.
     */
    public ZkConf() {

    }

    /**
     * Generates a random 10-digit positive zero-padded id.
     *
     * @return the 8-digit positive zero-padded id.
     */
    private String generateId() {
        long min = 0;
        long max = 9999999999L;
        long numId = min + (long) (new Random().nextDouble() * (max - min));
        String id = String.format("%010d", numId);
        return id;
    }

    /**
     * Initializes a (@link ZkNode) for a container type. The zookeeper path is
     * derived from two components: the zookeeper root + the type argument.
     *
     * @param type container type.
     */
    public void initZkContainerType(String type) {
        // create node's path
        String path = root.getPath() + "/" + type;
        // craete node's name
        String name = "/" + type;
        // create a new zk node object 
        ZkNode zkNode = new ZkNode(path, suffix.getBytes(), name, "");
        // add to list
        containerTypes.add(zkNode);
        zkAppNamespace.add(zkNode);
    }

    /**
     * Initializes a (@link ZkNode) for a container. The zookeeper path is
     * derived from three components: the zookeeper root + the type argument +
     * name argument.
     *
     * @param name the name of a container.
     * @param type the type of a container.
     * @param data the upData of the zkNode.
     */
    public void initZkContainer(String name, String type, byte[] data) {
        // create node's path
        String nodePath = root.getPath() + "/" + type + "/" + name;
        // craete node's name
        String nodeName = name;
        // create the path where the container's configuration will be stored to zk
        String conConfPath = conDesc.getPath() + "/" + name;
        // create a new zk node object
        ZkNode zkNode = new ZkNode(nodePath, data, nodeName, conConfPath);
        // add to list
        containers.put(name, zkNode);
    }

    /**
     * Creates the map of defined container names to deployed container names.
     *
     * @param deplCons the list of defined container names.
     */
    public void initDeplCons(List<String> deplCons) {
        // init map
        deplCons.stream().forEach((defName) -> {
            String deplName = defName + "-" + suffix;
            this.deplCons.put(defName, deplName);
        });
    }

    // Getters 
    public String getSuffix() {
        return suffix;
    }

    public ZkNode getRoot() {
        return root;
    }

    public ZkNode getServices() {
        return services;
    }

    public ZkNode getConDesc() {
        return conDesc;
    }

    public ZkNode getShutdown() {
        return shutdown;
    }

    public ZkNode getZkConf() {
        return zkConf;
    }

    public List<ZkNode> getContainerTypes() {
        return containerTypes;
    }

    public Map<String, ZkNode> getContainers() {
        return containers;
    }

    public List<ZkNode> getZkAppNamespace() {
        return zkAppNamespace;
    }

    public ZkSrvConf getZkSrvConf() {
        return zkSrvConf;
    }

    /**
     *
     * @return a Map of the defined container names as key and the deployed
     * container names as value.
     */
    public Map<String, String> getDeplCons() {
        return deplCons;
    }

    public ProgramConf getpConf() {
        return pConf;
    }

    public void setpConf(ProgramConf pConf) {
        this.pConf = pConf;
    }

    public WebApp getWebApp() {
        return webApp;
    }

    public void setWebApp(WebApp webApp) {
        this.webApp = webApp;
    }

    public Map<String, String> getConUpDataNodes() {
        return conUpDataNodes;
    }

    public String getAppDefaultNetName() {
        return appDefaultNetName;
    }

}
