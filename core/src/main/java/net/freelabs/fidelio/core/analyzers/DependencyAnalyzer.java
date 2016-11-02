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
package net.freelabs.fidelio.core.analyzers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.freelabs.fidelio.core.schema.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that provides methods to analyze container dependencies in order to
 * detect circular dependencies.
 */
public class DependencyAnalyzer {

    /**
     * List of containers declared on application schema.
     */
    private final List<Container> containers;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DependencyAnalyzer.class);

    /**
     * Constructor.
     *
     * @param containers list of containers declared in application description.
     */
    public DependencyAnalyzer(List<Container> containers) {
        this.containers = containers;
    }

    /**
     * <p>
     * Analyzes declared container dependencies for circular dependencies.
     * <p>
     * Calls recursive method {@link #detectCircular(net.freelabs.fidelio.core.generated.Container, java.util.List, java.util.Map)
     * detectCircular}.
     *
     * @return true if a circular dependency is found.
     */
    public boolean analyzeDependencies() {
        boolean found = false;
        // create map with the dependency lists of all contaienrs
        Map<String, List<String>> conDepMap = new HashMap<>();

        containers.stream().forEach((con) -> {
            String key = con.getConSrvName();
            List<String> value = con.getRequires();
            conDepMap.put(key, value);
        });
        // iterate through containers
        for (Container con : containers) {
            // if container has no dependencies skip it
            if (!con.getRequires().isEmpty()) {
                found = detectCircular(con.getConSrvName(), new ArrayList<>(), conDepMap);
                if (found) {
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Detects circular dependencies. Recursive method.
     *
     * @param src the container to check if it is declared as a circular
     * dependency.
     * @param depChain the list of dependencies forming a chain in which every
     * dependency must appear only once.
     * @param conDepMap map with the dependency lists of all containers.
     * @return true if a circular dependency is found.
     */
    private boolean detectCircular(String src, List<String> depChain, Map<String, List<String>> conDepMap) {
        // indicates if a circular dependency was found
        boolean found = false;
        // get dependency list of container
        List<String> dependencies = conDepMap.get(src);
        // check conditions
        if (depChain.contains(src)) {
            // container already exists in the dependency chain
            LOG.error("Container \'{}\' is declared as a CIRCULAR DEPENDENCY in application description.", src);
            found = true;
        } else {
            // add container to the list of depending nodes that were visited
            // and must appear only once
            depChain.add(src);
            // get dependency list of dependency and recurse
            for (String dep : dependencies) {
                // recurse
                List<String> depChainCopy = new ArrayList<>();
                depChainCopy.addAll(depChain);
                found = detectCircular(dep, depChainCopy, conDepMap);
                // if found circular dependency stop
                if (found) {
                    break;
                }
            }
        }
        return found;
    }
}
