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

/**
 *
 * Class that provides methods to map environments declared in application
 * description to {@link Environment environment} objects.
 */
public class EnvironmentMapper {

    /**
     * The object holding the environment information for the {@link Container
     * container} object associated with the Broker.
     */
    private final Environment conEnv;

    /**
     * A map, with the name of the containers dependencies to the container
     * associated with the Broker and the objects holding the environment
     * information for the {@link Container container} objects of the
     * dependencies.
     */
    private Map<String, Environment> depConEnvMap;

    public EnvironmentMapper(ContainerEnvironment conEnv, String conName, Map<String, ContainerEnvironment> depConEnvMap) {
        this.conEnv = new Environment(conName, conEnv);
        this.depConEnvMap = new HashMap<>();

        depConEnvMap.entrySet().stream().forEach((entry) -> {
            String key = entry.getKey();
            ContainerEnvironment value = entry.getValue();
            Environment env = new Environment(key, value);
            // put to map
            this.depConEnvMap.put(key, env);
        });
    }
    
    // Getters
    public Environment getConEnv() {
        return conEnv;
    }

    public Map<String, Environment> getDepConEnvMap() {
        return depConEnvMap;
    }
}
