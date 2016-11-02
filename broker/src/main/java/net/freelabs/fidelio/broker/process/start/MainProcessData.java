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
package net.freelabs.fidelio.broker.process.start;

import java.util.Map;
import net.freelabs.fidelio.broker.process.ProcessData;
import net.freelabs.fidelio.broker.process.Resource;

/**
 *
 * Class that provides methods to store and access all data necessary for the 
 * initialization of the main container process.
 */
public final class MainProcessData extends ProcessData {
    /**
     * The port the process is running.
     */
    private final  int procPort;
    /**
     * The IP of the host where the process is running.
     */
    private final String prochost;

    public MainProcessData(Resource res, Map<String, String> env, String prochost, int procPort) {
        super(res, env);
        this.prochost = prochost;
        this.procPort = procPort;
    }
    

    // Getters - Setters
    public int getProcPort() {
        return procPort;
    }

    public String getProchost() {
        return prochost;
    }

}
