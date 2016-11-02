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
package net.freelabs.fidelio.broker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * Class that provides configuration for {@link Broker Broker} class.
 */
public final class BrokerConf {
    
    public BrokerConf() {
        try {
            // create temporary restore dir
            Path tempDir = Files.createTempDirectory("restore");
            RESTORE_DIR = tempDir.toString();
            // create services restore dir
            Path tempDir2 = Files.createTempDirectory("servicesConf");
            SERVICES_DIR = tempDir2.toString();
        } catch (IOException ex) {
            RESTORE_DIR = PROGRAM_DIR + "/restore";
            SERVICES_DIR = PROGRAM_DIR + "/servicesConf";
        }
    }
    
    public static final String PROGRAM_DIR = "/opt/fidelio";
    public static String RESTORE_DIR;
    public static String SERVICES_DIR;
    public String brokerDir;
    
}
