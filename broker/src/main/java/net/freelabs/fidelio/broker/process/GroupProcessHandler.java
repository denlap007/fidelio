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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Template class to be sub-classed in order to create processHandlers for
 * groups of processes.
 */
public abstract class GroupProcessHandler {

    /**
     * Executes when group processes are started and initialized successfully.
     */
    protected Executable execOnSuccess;
    /**
     * Executes when group processes did not start or initialize (or both)
     * successfully.
     */
    protected Executable execOnFailure;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GroupProcessHandler.class);

    /**
     * Executes a group of processes. First, checks if group handler is
     * initialized and then it starts group process execution. When all
     * processes of the group have finished execution, additional code may be
     * executed based on the result outcome (success or failure).
     *
     * @return true if execution completed without errors
     */
    protected final boolean exec_group_procs() {
        boolean success = false;

        if (isGroupHandlerInitialized()) {
            success = start_group_procs();

            // execute code depending on group process execution sucess
            if (success) {
                if (execOnSuccess != null) {
                    execOnSuccess.execute();
                }
            } else if (execOnFailure != null) {
                execOnFailure.execute();
            }

            // cleanup
            cleanup();

        } else {
            LOG.error("Group Process Handler NOT INITIALIZED properly.");
        }

        return success;
    }

    /**
     * Starts group process execution.
     *
     * @return
     */
    protected abstract boolean start_group_procs();

    /**
     * Checks group process handler for initialization.
     *
     * @return
     */
    protected abstract boolean isGroupHandlerInitialized();

    /**
     * Perform cleanup operations.
     */
    protected abstract void cleanup();

    // Getters-Setters
    public Executable getExecOnSuccess() {
        return execOnSuccess;
    }

    public void setExecOnSuccess(Executable execOnSuccess) {
        this.execOnSuccess = execOnSuccess;
    }

    public Executable getExecOnFailure() {
        return execOnFailure;
    }

    public void setExecOnFailure(Executable execOnFailure) {
        this.execOnFailure = execOnFailure;
    }

}
