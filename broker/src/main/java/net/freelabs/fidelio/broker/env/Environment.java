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
package net.freelabs.fidelio.broker.env;

import java.util.HashMap;
import java.util.Map;
import net.freelabs.fidelio.core.schema.Container;
import net.freelabs.fidelio.core.schema.ContainerEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class whose instances hold information about the environment of a container
 * as declared in the application description.
 */
public class Environment {

    /**
     * The object holding the environment for a {@link Container
     * container} object.
     */
    private final ContainerEnvironment conEnv;
    /**
     * The name of the container to which the environment belongs.
     */
    private final String conName;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Environment.class);

    /**
     * Constructor.
     *
     * @param conName the name of the container to which the environment
     * belongs.
     * @param conEnv object holding the environment for a {@link Container
     * container} object.
     *
     */
    public Environment(String conName, ContainerEnvironment conEnv) {
        this.conEnv = conEnv;
        this.conName = conName;
    }

    /**
     * Returns the environment for a container, that is the environment
     * variables key-value pairs as declared in container description.
     *
     * @param prefix a prefix to be applied to every environment variable name.
     * @return the map with the environment variables key-value pairs.
     */
    public Map<String, String> getEnvMap(String prefix) {
        Map<String, String> envMap = new HashMap<>();

        for (Map.Entry<String, String> entry : conEnv.createEnvMap().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!prefix.isEmpty()) {
                key = (prefix + "_" + key).toUpperCase();
            } else {
                key = key.toUpperCase();
            }
            // add entry for new key to map 
            envMap.put(key, value);
        }
        return envMap;
    }

    public Map<String, String> getEnvMappings(String prefix) {
        Map<String, String> envMappings = new HashMap<>();

        for (Map.Entry<String, String> entry : conEnv.createEnvMappings().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!prefix.isEmpty()) {
                key = (prefix + "_" + key).toUpperCase();
            } else {
                key = key.toUpperCase();
            }
            // add entry for new key to map 
            envMappings.put(key, value);
        }
        return envMappings;
    }

    // Getters
    public ContainerEnvironment getConEnv() {
        return conEnv;
    }

    public String getConName() {
        return conName;
    }
}
