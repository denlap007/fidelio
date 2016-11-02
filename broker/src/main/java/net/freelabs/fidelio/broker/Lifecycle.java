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
package net.freelabs.fidelio.broker;

/**
 *
 * Interface that defines the life-cycle of {@link Broker Broker}.
 */
public interface Lifecycle {
    /**
     * Boots Broker.
     */
    public void boot();
    /**
     * Initializes Broker.
     */
    public void init();
    /**
     * Executes logic to start services, processes, tasks.
     */
    public void start();
    /**
     * Executes logic to stop services, processes, tasks.
     */
    public void stop();
    /**
     * Handles reconfiguration of container due to a service-dependency update.
     */
    public void update();
    /**
     * Handles execution in case of fatal errors.
     */
    public void error();
    /**
     * Handles Broker shutdown procedure.
     */
    public void shutdown();
}
