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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class template provides functionality to map resources. This class must be
 * sub-classed to implement custom functionality.
 *
 * @param <T1> the type argument for preMain and postMain resources (lists).
 * @param <T2> the type argument for the main resource.
 */
public abstract class ResourceMapper<T1, T2> {

    /**
     * A list with the resources to execute before the main resource.
     */
    protected final List<Resource> preMainRes = new ArrayList<>();

    /**
     * A list with the resources to execute after the main resource.
     */
    protected final List<Resource> postMainRes = new ArrayList<>();
    /**
     * The main resource to execute.
     */
    protected Resource mainRes;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMapper.class);
    
    /**
     * Constructor.
     */
    public ResourceMapper(){
        
    }

    /**
     * Initializes the Resources of mapper. Maps declared objects to Resources.
     *
     * @param preMain list with pre main resources.
     * @param postMain list with post main resources.
     * @param main main resource.
     */
    public abstract void initResources(List<T1> preMain, List<T1> postMain, T2 main);

    /**
     * Checks if a resource is defined and not empty.
     *
     * @param res the resource to check.
     * @return true if the resource is defined and not empty.
     */
    public boolean isResourceOk(Resource res) {
        boolean ok = false;

        if (res != null) {
            if (res.getRes().isEmpty()) {
                LOG.error("Resource defined but not initialized. Empty.");
            } else {
                ok = true;
            }
        } else {
            LOG.error("Resource NOT defined.");
        }
        return ok;
    }

    // Getters
    public Resource getMainRes() {
        return mainRes;
    }

    public List<Resource> getPreMainRes() {
        return preMainRes;
    }

    public List<Resource> getPostMainRes() {
        return postMainRes;
    }

}
