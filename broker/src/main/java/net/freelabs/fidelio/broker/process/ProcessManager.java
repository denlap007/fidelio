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

import java.util.concurrent.CountDownLatch;
import net.freelabs.fidelio.broker.process.start.StartGroupProcessHandler;
import net.freelabs.fidelio.broker.process.stop.StopGroupProcessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that manages process execution.
 */
public final class ProcessManager {

    /**
     * Process manager for processes defined in start section.
     */
    private StartGroupProcessHandler startGroupHandler;
    /**
     * Process manager for processes defined in stop section.
     */
    private StopGroupProcessHandler stopGroupHandler;
    /**
     * Flag to indicate if start group processes have executed.
     */
    private final CountDownLatch statrGroupExecutedSignal = new CountDownLatch(1);
    /**
     * Flag that indicates if start group processes have executed.
     */
    private volatile boolean statrGroupExecuted;

    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProcessManager.class);

    /**
     * Initializes process manager with a start group handler, that handles
     * execution of processes defined in start section.
     *
     * @param startGroupHandler an initialized instance of {@link #startGroupHandler
     * startGroupHandler).
     */
    public void setStartGroupHandler(StartGroupProcessHandler startGroupHandler) {
        synchronized (this) {
            this.startGroupHandler = startGroupHandler;
        }
    }

    /**
     * Initializes process manager with a stop group handler, that handles
     * execution of processes defined in stop section.
     *
     * @param stopGroupHandler an initialized instance of {@link #stopGroupHandler
     * stopGroupHandler}.
     */
    public void setStopGroupHandler(StopGroupProcessHandler stopGroupHandler) {
        synchronized (this) {
            this.stopGroupHandler = stopGroupHandler;
        }
    }

    /**
     * Executes processes defined in start section.
     */
    public void exec_start_procs() {
        LOG.info("Executing start-group processes.");
        if (isStartHandlerInit()) {
            boolean success = startGroupHandler.exec_group_procs();
            if (success) {
                LOG.info("Start-group processes executed SUCCESSFULLY.");
            }
        } else {
            LOG.error("Start-group processes handler NOT INITIALIZED.");
        }
        statrGroupExecutedSignal.countDown();
        statrGroupExecuted = true;
    }

    /**
     * Executes processes defined in stop section.
     */
    public void exec_stop_procs() {
        if (statrGroupExecuted) {
            LOG.info("Executing stop-group processes.");
            if (isStopHandlerInit()) {
                boolean success = stopGroupHandler.exec_group_procs();
                if (success) {
                    LOG.info("Stop-group processes executed SUCCESSFULLY.");
                } else {
                    LOG.error("Stop-group processes executed WITH ERRORS.");
                }
            } else {
                LOG.error("Stop-group processes handler NOT INITIALIZED.");
            }
        } else {
            LOG.info("Stop-group processes queued. Waiting for start-group to finish...");
            try {
                statrGroupExecutedSignal.await();
                exec_stop_procs();
            } catch (InterruptedException ex) {
                // log the event
                LOG.warn("Thread interrupted. Stopping.");
                // set the interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Checks if the main process is running.
     *
     * @return true if the main container process is running.
     */
    public boolean isMainProcRunning() {
        return startGroupHandler.isMainProcRunning();
    }

    /**
     * Waits until the main process stops running.
     */
    public void waitForMainProc() {
        startGroupHandler.waitForMainProc();
    }

    /**
     * Checks if handler for stop process group is initialized.
     *
     * @return true if handler for stop process group is initialized.
     */
    public boolean isStopHandlerInit() {
        synchronized (this) {
            return stopGroupHandler != null;
        }
    }

    /**
     * Checks if handler for start process group is initialized.
     *
     * @return true if handler for start process group is initialized.
     */
    public boolean isStartHandlerInit() {
        synchronized (this) {
            return startGroupHandler != null;
        }
    }
}
