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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * <p>
 * Class that provides methods to manage the life-cycle of a new process. The
 * class sets the required structure a {@link ProcessHandler ProcessHandler}
 * must have.
 * <p>
 * Subclasses may implement the required extra functionality.
 */
public abstract class ProcessHandler {

    /**
     * Holds all the data needed for the process handled by the
     * {@link ProcessHandler ProcessHandler}.
     */
    protected ProcessData pData;
    /**
     * The process object associated with the
     * {@link ProcessHandler ProcessHandler}.
     */
    protected Process _proc;
    /**
     * Initialize and starts a new process.
     */
    protected ProcessBuilder pb;
    /**
     * Executes when process is started and initialized successfully.
     */
    protected Executable execOnSuccess;
    /**
     * Executes when process did not start or initialize (or both) successfully.
     */
    protected Executable execOnFailure;
    /**
     * If process was successfully started and initialized.
     */
    protected boolean success;
    /**
     * A Logger object.
     */
    protected final Logger LOG = LoggerFactory.getLogger(ProcessHandler.class);

    /**
     * Constructor.
     *
     * @param pData the object holding all the required data for the new
     * process.
     */
    public ProcessHandler(ProcessData pData) {
        this.pData = pData;
    }

    /**
     * Creates a {@link ProcessBuilder ProcessBuilder} that will be used to
     * initialize and run a new process.
     */
    protected void create() {
        pb = new ProcessBuilder();
    }

    /**
     * <p>
     * Initializes a new process.
     * <p>
     * The method acts on data found on {@link ProcessData ProcessData} object.
     * Initializes the environment and sets the command and arguments to be
     * executed by the new process.
     * <p>
     * Subclasses should Override this method to implements extra functionality.
     *
     * @return true if process initialized without errors.
     */
    protected boolean init() {
        boolean initialized = false;
        // get the environmente of the new process
        Map<String, String> env = pb.environment();
        // add the necessary external environment defined in ProcessData obj
        env.putAll(pData.getEnvironment());
        // get a list with cmd and args
        List<String> procCmdArgs = pData.getCmdArgs();
        // if there were no errors
        if (!procCmdArgs.isEmpty()) {
            // set command and arguments
            pb.command(procCmdArgs);
            // set initialization flag to true
            initialized = true;
        }
        return initialized;
    }

    /**
     * Starts a new process.
     *
     * @return
     */
    protected abstract boolean start();

    /**
     * Stops the process handled.
     */
    public abstract void stop();

    /**
     * Perform cleanup operations.
     */
    protected abstract void cleanup();

    /**
     * <p>
     * Executes methods in succession: {@link #create() create}, {@link #init()
     * init} and {@link #start() start} to create, initialize and start the new
     * process accordingly.
     * <p>
     * The method waits for {@link #start() start} method to return.
     * <p>
     * The method blocks.
     *
     * @return true if process executed successfully. The process executed
     * successfully if {@link #start() start} method returned true.
     * Consequently, the return value matches the one from start method.
     */
    public final boolean execute() {
        if (isHandlerInitialized()) {
            // create Process Builder to initialize process
            create();
            // initialize process
            boolean initialized = init();
            // execute process if it is correctly initialized
            if (initialized) {
                success = start();
                // execute code depending on process execution sucess or not
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
                LOG.error("Process initialization FAILED: {}.", pData.getResDescription());
            }
        } else {
            LOG.error("Process Handler NOT INITIALIZED properly.");
        }

        return success;
    }

    /**
     * <p>
     * Checks if {@link ProcessHandler ProcessHandler} is initialized properly.
     * <p>
     * In order to be properly initialized, field {@link #pData pData},
     * {@link #execOnSuccess execOnSuccess} must be set.
     *
     * @return true if handler is properly initialized.
     */
    protected boolean isHandlerInitialized() {
        return (pData != null);
    }

    /**
     * <p>
     * Returns the process pid through reflection.
     * <p>
     * If there is a {@link SecurityManager SecurityManager} installed, this
     * method may throw an exception.
     *
     * @return the process pid.
     */
    public final int getProcPid() {
        int pid = -1;

        try {
            Field pidField = _proc.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            pid = pidField.getInt(_proc);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            LOG.error("Something went wrong: " + ex);
        }

        return pid;
    }

    /**
     *
     * @return true if process execution was successful. False in case
     * {@link ProcessHandler ProcessHandler} was not properly initialized, the
     * process did not start or initialize successfully.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Set code to execute if process was executed successfully.
     *
     * @param execOnSuccess
     */
    public void setExecOnSuccess(Executable execOnSuccess) {
        this.execOnSuccess = execOnSuccess;
    }

    /**
     * Set code to execute if process execution failed.
     *
     * @param execOnFailure
     */
    public void setExecOnFailure(Executable execOnFailure) {
        this.execOnFailure = execOnFailure;
    }

    public ProcessData getpData() {
        return pData;
    }
}
