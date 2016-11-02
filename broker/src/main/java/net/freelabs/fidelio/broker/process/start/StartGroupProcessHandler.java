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

import java.util.List;
import net.freelabs.fidelio.broker.process.GroupProcessHandler;
import net.freelabs.fidelio.broker.process.ProcessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that manages process execution defined in start.
 */
public final class StartGroupProcessHandler extends GroupProcessHandler {

    /**
     * The handler for the main process.
     */
    private final MainProcessHandler mainHandler;
    /**
     * List of handlers for other processes to be executed before the main
     * process.
     */
    private final List<ProcessHandler> preMainHandlers;
    /**
     * List of handlers for other processes to be executed after the main
     * process is executed successfully.
     */
    private final List<ProcessHandler> postMainHandlers;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StartGroupProcessHandler.class);

    /**
     * Constructor.
     *
     * @param preHandlers list of handlers for processes to be executed before
     * the main process.
     * @param postHandlers list of handlers for processes to be executed after
     * the main process.
     * @param mainHandler the handler for the main process.
     */
    public StartGroupProcessHandler(List<ProcessHandler> preHandlers, List<ProcessHandler> postHandlers, MainProcessHandler mainHandler) {
        preMainHandlers = preHandlers;
        postMainHandlers = postHandlers;
        this.mainHandler = mainHandler;

    }

    /**
     * <p>
     * Starts executing declared processes.
     * <p>
     * If declared, any processes to be run before the main are executed. Then,
     * the main container process is started. If executed successfully the other
     * processes are spawned.
     * <p>
     * The method waits for every process to finish execution, except main. Main
     * process returns after initialization is complete. That is because a main
     * process will be running a server. When the server is initialized and
     * running we don't want the process to finish execution as the server will
     * stop running.
     * <p>
     * The method blocks.
     *
     * @return
     */
    @Override
    protected boolean start_group_procs() {
        boolean preMainSuccess = true;
        boolean postMainSuccess = true;
        boolean mainSuccess = true;

        // execute pre-main processes, if any
        if (!preMainHandlers.isEmpty()) {
            for (ProcessHandler procHandler : preMainHandlers) {
                // execute process and get success value
                preMainSuccess = procHandler.execute();
                // get user requirement in case of failure
                boolean abortOnFail = procHandler.getpData().getRes().isAbortOnFail();
                // if there was an error and user concurs exit
                if (!preMainSuccess && abortOnFail) {
                    break;
                } else {
                    // correct flag if error is ignored to execute main proc
                    preMainSuccess = true;
                }
            }
        }

        // if preMain procs executed successfully, execute main
        if (preMainSuccess) {
            // execute the main Process
            mainSuccess = mainHandler.execute();

            // if main proc executed successfully, execute postMain procs
            if (mainSuccess) {
                // execute post-main processes, if any
                if (!postMainHandlers.isEmpty()) {
                    for (ProcessHandler procHandler : postMainHandlers) {
                        // execute process and get success value
                        postMainSuccess = procHandler.execute();
                        // get user requirement in case of failure
                        boolean abortOnFail = procHandler.getpData().getRes().isAbortOnFail();
                        // if there was an error and user concurs exit
                        if (!postMainSuccess && abortOnFail) {
                            break;
                        } else {
                            // correct flag if error is ignored
                            postMainSuccess = true;
                        }
                    }
                    // check for errors on postMain proc execution to abort
                    if (!postMainSuccess) {
                        LOG.error("Post-main process execution FAILED.");
                        mainHandler.stop();
                    }
                }
            } else {
                LOG.error("ABORTING post-main process execution.");
            }
        } else {
            LOG.error("Pre-main process execution FAILED. "
                    + "ABORTING main process execution.");
        }

        return preMainSuccess && postMainSuccess && mainSuccess;
    }

    @Override
    protected boolean isGroupHandlerInitialized() {
        boolean initialized = false;
        if (mainHandler == null) {
            LOG.error("Start Group Process Handler CANNOT start: "
                    + "Main Process Handler NOT INITIALIZED properly.");
        } else if (postMainHandlers == null && preMainHandlers == null) {
            LOG.error("Start Group Process Handler CANNOT start: "
                    + "PreMain and PostMain Process Handlers NOT INITIALIZED properly.");
        } else if (preMainHandlers == null) {
            LOG.error("Start Group Process Handler CANNOT start: "
                    + "PreMain Process Handler NOT INITIALIZED properly.");
        } else if (postMainHandlers == null) {
            LOG.error("Start Group Process Handler CANNOT start: "
                    + "PostMain Process Handler NOT INITIALIZED properly.");
        } else {
            initialized = true;
        }
        return initialized;
    }

    /**
     * Checks if the main process is running.
     *
     * @return true if the main container process is running.
     */
    public boolean isMainProcRunning() {
        return mainHandler.isMainProcRunning();
    }

    /**
     * Waits until the main process stops running.
     */
    public void waitForMainProc() {
        mainHandler.waitForMainProc();
    }

    @Override
    protected void cleanup() {
    }
}
