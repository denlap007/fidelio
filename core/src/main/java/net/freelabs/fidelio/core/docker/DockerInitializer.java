/*
 * Copyright (C) 2015 Dionysis Lappas <dio@freelabs.net>
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
package net.freelabs.fidelio.core.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that creates and initializes a docker client for the App to communicate
 * with the docker daemon.
 */
public final class DockerInitializer {

    /**
     * The docker client that will communicate with the docker daemon.
     */
    private final DockerClient dockerClient;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DockerInitializer.class);

    /**
     * Constructor.
     *
     * @param dockerArgs the initialization parameters of the docker client.
     * dockerArgs[0] --> docker host uri dockerArgs[1] --> enable/disable TLS
     * verification (switch between http and https protocol) dockerArgs[2] -->
     * path to the certificates needed for TLS verification dockerArgs[3] -->
     * path for additional docker configuration files (like .dockercfg)
     * dockerArgs[4] --> the API version, e.g. 1.21 dockerArgs[5] --> your
     * registry's address dockerArgs[6] --> your registry username (required to
     * push containers) dockerArgs[7] --> your registry password dockerArgs[8]
     * --> your registry email
     */
    public DockerInitializer(String... dockerArgs) {
        dockerClient = initDockerClient(dockerArgs);
    }

    /**
     * Creates and initializes a docker client.
     *
     * @param dockerURI the docker daemon uri.
     * @return the docker client object.
     */
    private DockerClient initDockerClient(String... dockerArgs) {
        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerArgs[0])
                .withDockerTlsVerify(dockerArgs[1])
                .withDockerCertPath(dockerArgs[2])
                .withDockerConfig(dockerArgs[3])
                .withApiVersion(dockerArgs[4])
                .withRegistryUrl(dockerArgs[5])
                .withRegistryUsername(dockerArgs[6])
                .withRegistryPassword(dockerArgs[7])
                .withRegistryEmail(dockerArgs[8])
                .build();

        DockerClient client = DockerClientBuilder.getInstance(config)
                .build();

        return client;
    }

    /**
     * Prints info of the docker client.
     */
    public void printDockerClientInfo() {
        Info info = getDockerClient().infoCmd().exec();
        LOG.info("Docker client info: " + info);
    }

    /**
     * @return the dockerClient.
     */
    public DockerClient getDockerClient() {
        return dockerClient;
    }
}
