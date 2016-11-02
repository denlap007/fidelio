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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 *
 * Class that represents a zookeeper Node.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@XmlAccessorType(XmlAccessType.FIELD)
public final class ZkNode {

    /**
     * The path of the zookeeper node.
     */
    private String path;
    /**
     * The data of the zookeeper node.
     */
    private byte[] data;
    /**
     * The name of the zookeeper node.
     */
    private String name;
    /**
     * The path of the zookeeper node with the configuration for this node.
     */
    private String confNodePath;

    /**
     * Constructor.
     *
     * @param path the path of the zookeeper node.
     * @param data the data of the zookeeper node.
     * @param name the name of the zookeeper node.
     * @param confNodePath the path of the zookeeper node with the user
     * configuration for this container-node.
     */
    @JsonCreator
    public ZkNode(
            @JsonProperty("path") String path,
            @JsonProperty("data") byte[] data, 
            @JsonProperty("name") String name, 
            @JsonProperty("confNodePath") String confNodePath) {
        this.path = path;
        this.data = data;
        this.name = name;
        this.confNodePath = confNodePath;
    }
    /**
     * FOR JAXB compatibility
     */
    public ZkNode(){
        
    }
    
    /**
     * @return the path of the zNode.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the data of the zNode.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @return the name of the zNode.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the confNodePath
     */
    public String getConfNodePath() {
        return confNodePath;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }
}
