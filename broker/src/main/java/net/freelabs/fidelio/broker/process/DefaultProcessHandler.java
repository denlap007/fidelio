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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 *
 * Class that provides a default implementation for a
 * {@link ProcessHandler ProcessHandler}.
 */
public class DefaultProcessHandler extends ProcessHandler {

    /**
     * Constructor.
     *
     * @param pData the object holding all the required data for the new
     * process.
     */
    public DefaultProcessHandler(ProcessData pData) {
        super(pData);
    }
    /**
     * Indicates if the process execution was successful.
     */
    private boolean succeeded;
    /**
     * Indicates if the process exitedOnTime before the timeout.
     */
    private boolean exitedOnTime;
    /**
     * Time to wait for execution to complete before aborting process measured
     * in {@link #TIME_UNIT TIME_UNITs}.
     */
    private static final long EXEC_TIMEOUT = 2;
    /**
     * The unit of time that applies to {@link #EXEC_TIMEOUT EXEC_TIMEOUT}.
     */
    private static final TimeUnit MINUTES = TimeUnit.MINUTES;

    /**
     * <p>
     * Initializes a new process.
     * <p>
     * The method Overrides the {@link ProcessHandler#init() init} method.
     * <p>
     * Redirects the stderr and stdout stream to the parent process.
     *
     * @return true if process initialized without errors.
     */
    @Override
    protected boolean init() {
        boolean initialized = super.init();
        // redirect error stream and output stream
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        return initialized;
    }

    @Override
    protected boolean start() {
        try {
            // start the new process
            _proc = pb.start();
            // log the event
            LOG.info("Started process: {}", pData.getResDescription());
            // wait for execution to complete 
            exitedOnTime = _proc.waitFor(EXEC_TIMEOUT, MINUTES);
            // get the exit code
            int errCode = _proc.exitValue();
            // if exited before timeout and exit code is 0, proc exec successful
            if (errCode == 0 && exitedOnTime == true) {
                LOG.info("Process executed successfully: {}", pData.getResDescription());
                succeeded = true;
            }else if (!exitedOnTime){
                LOG.error("Process execution TIMED OUT: {}", pData.getResDescription());
            }else if (errCode != 0 ){
                LOG.error("Process execution FAILED: {}. Exit Code: {}.", pData.getResDescription(), errCode);
            }
        } catch (IOException ex) {
            LOG.error("FAILED to start process: " + ex);
        } catch (InterruptedException ex) {
            LOG.warn("Thread interrupted. Stopping.");
            Thread.currentThread().interrupt();
        }
        return succeeded;
    }

    @Override
    public void stop() {
        LOG.warn("STOPPING process: {}", pData.getResDescription());
        _proc.destroyForcibly();
    }

    @Override
    protected void cleanup() {
    }

}
