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
package net.freelabs.fidelio.core.handlers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class whose instances handle the interaction with the networks for the
 * deployed application. The class provides methods to create new networks for
 * the app.
 */
public final class NetworkHandler {

    /**
     * Instance of docker client for communication with the docker hsot.
     */
    private final DockerClient docker;
    /**
     * The id the network for the application.
     */
    private String appNetId;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NetworkHandler.class);

    /**
     * Constructor
     *
     * @param docker instance of docker client for communication with the docker
     * host.
     */
    public NetworkHandler(DockerClient docker) {
        this.docker = docker;
    }

    /**
     * Creates a new network. The new network name is generated from the appId
     * concatenated with the {@link #NET_NAME_SUFFIX NET_NAME_SUFFIX}.
     *
     * @param netName the name of the network to create.
     * @return true if the network was created without errors.
     */
    public boolean createNetwork(String netName) {
        // create default network for app. 
        LOG.info("Creating network {} with the default driver...", netName);
        CreateNetworkResponse createNetworkResponse = null;
        try {
            createNetworkResponse = docker.createNetworkCmd()
                    .withName(netName)
                    .exec();
        } catch (Exception ex) {
            LOG.error("Something went wrong: {}", ex.getMessage());
            LOG.trace("Something went wrong: ", ex);
        }

        if (createNetworkResponse != null) {
            appNetId = createNetworkResponse.getId();
        }

        return appNetId != null;
    }

    public boolean deleteNetwork(String netName) {
        boolean success = false;
        LOG.info("Removing network {}...", netName);
        try {
            docker.removeNetworkCmd(netName).exec();
            success = true;
        } catch (Exception ex) {
            LOG.error("Something went wrong: {}", ex.getMessage());
            LOG.trace("Something went wrong: ", ex);
        }
        return success;
    }

    public String getAppNetId() {
        return appNetId;
    }
}
