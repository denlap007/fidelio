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
package net.freelabs.fidelio.broker.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;

/**
 *
 * <p>
 * Class that handles all the configuration for the initialization and run of a
 * new process.
 * <p>
 * The class provides the basic structure of a data object for a new process.
 * <p>
 * Subclasses should implement extra functionality.
 */
public class ProcessData {

    /**
     * A resource to execute.
     */
    private final Resource res;
    /**
     * The environment of the process, which includes the necessary environment
     * for the main process to initialize and the environment from the required
     * services.
     */
    private final Map<String, String> environment;

    /**
     * A Logger object.
     */
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ProcessData.class);

    /**
     * Constructor.
     *
     * @param res a resource to execute.
     * @param env the environment of the process.
     */
    public ProcessData(Resource res, Map<String, String> env) {
        this.res = res;
        if (env == null) {
            environment = new HashMap<>();
        } else {
            environment = env;
        }
    }

    // Getters - Setters
    public Resource getRes() {
        return res;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * <p>
     * Gets the command and arguments of an execution resource, in order to be
     * used for a new process initialization.
     * <p>
     * First, if there are any environmnet variables in the execution resource
     * they are expanded.
     * <p>
     * Second, the execution resource is split into tokens.
     * <p>
     * If any errors occur, an empty list is returned.
     *
     * @return a list with the command and arguments of the resource.
     */
    public List<String> getCmdArgs() {
        List<String> cmdArgs = new ArrayList<>();

        LOG.debug("Resource BEFORE processing: {}", res.getRes());
        // expands the environment variables found in the execution resource, if any
        boolean expandedEnvVars = substEnvVarsToRes();
        // if no errors split into tokens
        if (expandedEnvVars) {
            cmdArgs = res.getResCmdArgs();
            if (cmdArgs.isEmpty()) {
                LOG.error("NO resource for execution.");
            }
        }
        LOG.debug("Resource AFTER processing: {}", res.getRes());
        return cmdArgs;
    }

    /**
     * <p>
     * Returns the resource description. The resource description is the path of
     * the script or the command that this resource represents. Used mainly for
     * logging purposes.
     * <p>
     * If the resource is not initialized an empty string is returned.
     *
     * @return the description of the resource.
     */
    public String getResDescription() {
        return res.getDescription();
    }

    /**
     * Substitutes environment variables of the format ${ENV_VAR} to an
     * execution resource.
     *
     * @param resource the execution resource to expand environment variables.
     * @return the execution resource with expanded environment variables. NULL
     * in case an environment variable cannot be expanded.
     */
    private boolean substEnvVarsToRes() {
        boolean success = true;
        String resource = res.getRes();
        final Pattern regex = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");

        // create object to match pattern
        Matcher regexMatcher = regex.matcher(resource);
        while (regexMatcher.find()) {
            // get the name of the environment variable matched, eg: $test->test
            String envVarName = regexMatcher.group(1);
            // search the environment for the variable name and get value
            String envVarValue = environment.get(envVarName);
            // check if there was such variable
            if (envVarValue != null) {
                // expand environment variabe
                String envVar = regexMatcher.group();
                resource = resource.replace(envVar, envVarValue);
                res.setRes(resource);
                LOG.debug("AFTER SUBST_ENV: {}", res.getRes());
            } else {
                LOG.error("FAILED to expand environment variable {}. Variable NOT DECLARED.", regexMatcher.group());
                success = false;
            }
        }

        return success;
    }
}
