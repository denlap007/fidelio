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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that handles the interaction with the environment of a {@link Container
 * container} object.
 */
public final class EnvironmentHandler {

    /**
     * The object holding the environment for the {@link Container
     * container} object associated with the Broker.
     */
    private final Environment conEnv;

    /**
     * A map, with the name of the containers dependencies to the container
     * associated with the Broker and the objects holding the environment for
     * the {@link Container container} objects of the dependencies.
     */
    private final Map<String, Environment> depConEnvMap;
    /**
     * The created environment for the container processes.
     */
    private Map<String, String> procsEnv;

    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentHandler.class);

    /**
     * Constructor.
     *
     * @param conEnv the object holding the environment for the {@link Container
     * container} object associated with the Broker.
     * @param depConEnvMap a map, with the name of the containers dependencies
     * to the container associated with the Broker and the objects holding the
     * environment for the {@link Container container} objects of the
     * dependencies.
     */
    public EnvironmentHandler(Environment conEnv, Map<String, Environment> depConEnvMap) {
        this.conEnv = conEnv;
        this.depConEnvMap = depConEnvMap;
    }

    /**
     * <p>
     * Creates the environment for all the container processes.
     * <p>
     * The environment consists of all the environment variables defined in the
     * application schema for the container associated with the Broker and all
     * its required dependencies.
     * <p>
     * The environment variables created comply to the following naming
     * convention format: ${containerName_envVarSchemaName}, with sole exception
     * the env vars declared to the container object associated with the Broker.
     * These env vars are not prefixed with the 'containerName'.
     * <p>
     * Example: <br>
     * Declared app consists of three containers with dependencies as follows:
     * data &lt;- business &lt;- web. <br>
     * Env var declarations:
     * <ul>
     * <li>data: db_name</li>
     * <li>business: app_name</li>
     * <li>web: host_port</li>
     * </ul>
     * Now, the created env vars in every container are as follows:
     * <ul>
     * <li>data: DB_NAME</li>
     * <li>business: APP_NAME, DATA_DB_NAME</li>
     * <li>web: HOST_PORT, BUSINESS_APP_NAME</li>
     * </ul>
     *
     * @return the environment for container processes.
     */
    public final Map<String, String> createProcsEnv() {
        // get environment from the container obj associated with the broker
        Map<String, String> envOfProcs = conEnv.getEnvMap("");
        // get environment from dependencies and add to environment
        envOfProcs.putAll(getDependenciesEnv());
        // process mappings of env vars to other env var names
        envOfProcs.putAll(processEnvMappings(envOfProcs, conEnv, ""));
        // initialize var with procs environment
        procsEnv = envOfProcs;
        return envOfProcs;
    }

    /**
     * Extracts the environment from the services-dependencies of the main
     * container service.
     *
     * @return a map with all the environment variables defines in services-
     * dependencies.
     */
    private Map<String, String> getDependenciesEnv() {
        LOG.info("Extracting environment from dependencies.");
        // holds the global key-value entries for all container
        Map<String, String> dependenciesEnv = new HashMap<>();
        // holds data per repetition
        Map<String, String> env;

        // iterate through the container dependencies
        for (Map.Entry<String, Environment> entry : depConEnvMap.entrySet()) {
            String depName = entry.getKey();
            Environment envObj = entry.getValue();
            // get environment
            env = envObj.getEnvMap(depName);
            // print extracted environment
            LOG.info("Environment of dependency: {}", depName);
            for (Map.Entry<String, String> e : env.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                LOG.info("{}={}", key, value);
            }
            // add map to dependencies map
            dependenciesEnv.putAll(env);
            // remove everything
            env.clear();
        }
        return dependenciesEnv;
    }

    private Map<String, String> processEnvMappings(Map<String, String> envOfProcs, Environment conEnv, String prefix) {
        Map<String, String> newMappings = new HashMap<>();
        Map<String, String> envMappings = conEnv.getEnvMappings(prefix);
        LOG.info("Checking for env var mappings.");
        for (Map.Entry<String, String> entry : envMappings.entrySet()) {
            String newEnvVar = entry.getKey();
            String mappedEnvVar = entry.getValue();
            // serch env for a key that matches the target
            if (envOfProcs.containsKey(mappedEnvVar)) {
                String mappedEnvVarValue = envOfProcs.get(mappedEnvVar);
                // add the mapping to the environment
                newMappings.put(newEnvVar, mappedEnvVarValue);
                LOG.info("Mapping {}:{} to {}:{}", newEnvVar,  mappedEnvVar, newEnvVar, mappedEnvVarValue);
            }else{
                LOG.warn("CANNOT complete mapping {}:{}. Env var {} does NOT exist.", newEnvVar, mappedEnvVar, mappedEnvVar);
            }
        }
        return newMappings;
    }

    /**
     *
     * @return the environment with which processes are initialized.
     */
    public Map<String, String> getProcsEnv() {
        return procsEnv;
    }
}
