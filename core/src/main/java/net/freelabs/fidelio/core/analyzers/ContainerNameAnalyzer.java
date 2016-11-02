/*
 * Copyright (C) 2016 Dionysis Lappas <dio@freelabs.net>
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
import java.util.List;
import net.freelabs.fidelio.core.schema.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that provides methods to check uniqueness of container names.
 */
public class ContainerNameAnalyzer {

    /**
     * List of containers declared on schema.
     */
    private final List<Container> containers;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DependencyAnalyzer.class);

    /**
     * Constructor.
     *
     * @param containers list of containers declared on schema.
     */
    public ContainerNameAnalyzer(List<Container> containers) {
        this.containers = containers;
    }

    /**
     * Detects duplicate container names.
     *
     * @return true if a duplicate container name is found.
     */
    public boolean detectDuplicateNames() {
        // indicates if duplicate container name is found
        boolean found = false;
        // list to hold all the container names
        List<String> nameList = new ArrayList<>();
        // add container names to list and check for repeating names
        for (Container con : containers) {
            // get the container name
            String name = con.getConSrvName();
            // check if name is already in name list
            if (nameList.contains(name)) {
                LOG.error("DUPLICATE container name found: {}", name);
                found = true;
                break;
            } else {
                nameList.add(name);
            }
        }
        return found;
    }
}
