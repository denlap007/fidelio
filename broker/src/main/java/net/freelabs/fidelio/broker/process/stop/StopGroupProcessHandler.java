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
package net.freelabs.fidelio.broker.process.stop;

import java.util.List;
import net.freelabs.fidelio.broker.process.GroupProcessHandler;
import net.freelabs.fidelio.broker.process.ProcessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that manages stop process execution defined in stop
 */
public final class StopGroupProcessHandler extends GroupProcessHandler {

    /**
     * List with all handlers.
     */
    private final List<ProcessHandler> handlers;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StopGroupProcessHandler.class);

    /**
     * Constructor.
     *
     * @param handlers list of process handlers of the processes to execute.
     */
    public StopGroupProcessHandler(List<ProcessHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * <p>
     * Starts executing declared processes. If processes executed successfully
     * the return value is set to true. However, If a process fails, remaining
     * processes will still be executed but the return value is set to false.
     * <p>
     * Method waits for processes to execute. Processes are executed serially.
     * <p>
     * Method blocks.
     *
     * @return true if group processes executed without errors.
     */
    @Override
    protected boolean start_group_procs() {
        boolean processesSuccess = true;

        for (ProcessHandler handler : handlers) {
            processesSuccess = handler.execute() && processesSuccess;
        }
        return processesSuccess;
    }

    @Override
    protected boolean isGroupHandlerInitialized() {
        boolean initialized = false;
        if (handlers == null) {
            LOG.error("Stop Group Process Handler CANNOT start: "
                    + "Process Handlers NOT INITIALIZED properly.");
        } else {
            initialized = true;
        }
        return initialized;
    }

    @Override
    protected void cleanup() {
    }
}
