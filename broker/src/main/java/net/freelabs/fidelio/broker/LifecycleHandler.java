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

import net.freelabs.fidelio.broker.process.Executable;
import net.freelabs.fidelio.broker.services.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that provides methods to manage the execution during different states
 * of the container life-cycle.
 */
public class LifecycleHandler {

    /**
     * The possible states of the life-cycle of the container.
     */
    private enum STATE {
        BOOT, INIT, START, UPDATE, SHUTDOWN, ERROR
    };

    private enum EVENT {
        BOOT, ERROR, SRV_INITIALIZED, SRV_NOT_INITIALIZED, SRV_NOT_RUNNING, SRV_UPDATED,
        SRV_DELETED, SRV_ADDED, SRV_NONE, CON_UPDATED, CON_INIT, SHUTDOWN
    };
    /**
     * Holds configuration and state status for all required services.
     */
    private ServiceManager srvMngr;
    /**
     * Flag indicating if services are processed.
     */
    private boolean srvsProcessed;
    /**
     * Indicates if the current life-cycle needs to be executed, or just stay at
     * that state.
     */
    private boolean execCycle;
    /**
     * The current container life-cycle state.
     */
    private STATE curState;
    /**
     * The event that was triggered.
     */
    private EVENT curEvent;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleHandler.class);

    /**
     * Calculates the transition to the next life-cycle state, based on the
     * current life-cycle state and other needed conditions.
     */
    private void transition() {
        if (curEvent == EVENT.BOOT) {
            LOG.debug("Current State: {}", curState.toString());
            curState = STATE.BOOT;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curEvent == EVENT.SHUTDOWN) {
            LOG.debug("Current State: {}", curState.toString());
            curState = STATE.SHUTDOWN;
            execCycle = true;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curEvent == EVENT.ERROR) {
            LOG.debug("Current State: {}", curState.toString());
            curState = STATE.ERROR;
            execCycle = true;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.BOOT && curEvent == EVENT.CON_INIT) {
            LOG.debug("Current State: {}", curState.toString());
            execCycle = true;
            curState = STATE.INIT;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.INIT
                && (curEvent == EVENT.SRV_ADDED || curEvent == EVENT.SRV_INITIALIZED)) {
            LOG.debug("Current State: {}", curState.toString());
            if (srvsProcessed) {
                // check if srvs are initialized
                boolean initialized = srvMngr.areSrvInitialized();
                if (initialized) {
                    execCycle = true;
                    curState = STATE.START;
                } else {
                    execCycle = false;
                }
            } else {
                // check if srvs are processed
                srvsProcessed = srvMngr.areSrvProcessed();
                // check if srvs are initialized
                boolean initialized = srvMngr.areSrvInitialized();
                if (initialized) {
                    execCycle = true;
                    curState = STATE.START;
                } else {
                    execCycle = false;
                }
            }
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.INIT && curEvent == EVENT.SRV_NONE) {
            LOG.debug("Current State: {}", curState.toString());
            execCycle = true;
            curState = STATE.START;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.INIT && curEvent == EVENT.SRV_UPDATED) {
            LOG.debug("Current State: {}", curState.toString());
            // check if srvs are processed
            srvsProcessed = srvMngr.areSrvProcessed();
            if (srvsProcessed) {
                // check if srvs are initialized
                boolean initialized = srvMngr.areSrvInitialized();
                if (initialized) {
                    execCycle = true;
                    curState = STATE.START;
                } else {
                    execCycle = false;
                }
            } else {
                execCycle = false;
            }
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.INIT
                && (curEvent == EVENT.SRV_DELETED || curEvent == EVENT.SRV_NOT_RUNNING
                || curEvent == EVENT.CON_UPDATED)) {
            LOG.debug("Current State: {}", curState.toString());
             execCycle = true;
            curState = STATE.ERROR;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.START
                && (curEvent == EVENT.SRV_DELETED || curEvent == EVENT.SRV_NOT_RUNNING
                || curEvent == EVENT.SRV_NOT_INITIALIZED)) {
            LOG.debug("Current State: {}", curState.toString());
             execCycle = true;
            curState = STATE.ERROR;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.START && curEvent == EVENT.SRV_UPDATED) {
            LOG.debug("Current State: {}", curState.toString());
            // check if srvs are processed
            srvsProcessed = srvMngr.areSrvProcessed();
            if (srvsProcessed) {
                // check if srvs are initialized (an updated service is initialized)
                boolean initialized = srvMngr.areSrvInitialized();
                if (initialized) {
                    execCycle = true;
                    curState = STATE.UPDATE;
                } else {
                    execCycle = false;
                }
            } else {
                execCycle = false;
            }
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.START && curEvent == EVENT.CON_UPDATED) {
            LOG.debug("Current State: {}", curState.toString());
            execCycle = true;
            curState = STATE.UPDATE;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.UPDATE && (curEvent == EVENT.SRV_DELETED || curEvent == EVENT.SRV_NOT_RUNNING
                || curEvent == EVENT.SRV_NOT_INITIALIZED)) {
            LOG.debug("Current State: {}", curState.toString());
            curState = STATE.ERROR;
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.UPDATE && curEvent == EVENT.SRV_UPDATED) {
            LOG.debug("Current State: {}", curState.toString());
            // check if srvs are processed
            srvsProcessed = srvMngr.areSrvProcessed();
            if (srvsProcessed) {
                // check if srvs are initialized (an updated service is initialized)
                boolean initialized = srvMngr.areSrvInitialized();
                if (initialized) {
                    execCycle = true;
                    curState = STATE.UPDATE;
                } else {
                    execCycle = false;
                }
            } else {
                execCycle = false;
            }
            LOG.debug("Next State: {}", curState.toString());
        } else if (curState == STATE.UPDATE && curEvent == EVENT.CON_UPDATED) {
            LOG.debug("Current State: {}", curState.toString());
            execCycle = true;
            curState = STATE.UPDATE;
            LOG.debug("Next State: {}", curState.toString());
        }
    }

    /**
     * Executes an action based on the calculated container life-cycle state.
     */
    private void action() {
        switch (curState) {
            case BOOT:
                execContainerBootCycle.execute();
                break;
            case INIT:
                if (execCycle) {
                    execCycle = false;
                    execContainerInitCycle.execute();
                }
                break;
            case START:
                if (execCycle) {
                    execCycle = false;
                    execContainerStartLifeCycle.execute();
                }
                break;
            case UPDATE:
                if (execCycle) {
                    execCycle = false;
                    execContainerUpdateLifeCycle.execute();
                }
                break;
            case SHUTDOWN:
                if (execCycle) {
                    execContainerShutdownLifeCycle.execute();
                    execCycle = false;
                }
                break;
            case ERROR:
                if (execCycle) {
                    execContainerErrorLifeCycle.execute();
                    execCycle = false;
                }
                break;
            default:
                LOG.error("An UNEXPECTED state has occured!");
                break;
        }
    }
    // ------- Executables for the different container life-cycle states ------- 
    /**
     * Executes code that will start the bootEvent cycle of the container
     * life-cycle .
     */
    private Executable execContainerBootCycle;
    /**
     * Executes code that will start the init cycle of the container life-cycle
     * .
     */
    private Executable execContainerInitCycle;
    /**
     * Executes code that will start the start cycle of the container
     * life-cycle.
     */
    private Executable execContainerStartLifeCycle;
    /**
     * Executes code that will start the update cycle of the container
     * life-cycle.
     */
    private Executable execContainerUpdateLifeCycle;
    /**
     * Executes code that will start the shutdown cycle of the container
     * life-cycle.
     */
    private Executable execContainerShutdownLifeCycle;
    /**
     * Executes code that will start the errorEvent cycle of the container
     * life-cycle.
     */
    private Executable execContainerErrorLifeCycle;

    // --------------------------- Events to interact --------------------------
    public synchronized void bootEvent() {
        // check LifecycleHandler initialization
        if (execContainerInitCycle != null && execContainerStartLifeCycle != null
                && execContainerShutdownLifeCycle != null && execContainerUpdateLifeCycle != null
                && execContainerErrorLifeCycle != null) {
            curEvent = EVENT.BOOT;
            curState = STATE.BOOT;
            transition();
            action();
        } else {
            LOG.error("LifecycleHandler NOT INITIALIZED.");
        }
    }

    /**
     * Signals a shutdown event.
     */
    public void shutdownEvent() {
        LOG.info("EVENT triggered: SHUTDOWN.");
        synchronized (this) {
            curEvent = EVENT.SHUTDOWN;
            transition();
        }
        action();
    }

    /**
     * Signals a service added event.
     */
    public void serviceAddedEvent() {
        LOG.info("EVENT triggered: SRV_ADDED.");
        synchronized (this) {
            curEvent = EVENT.SRV_ADDED;
            transition();
        }
        action();
    }

    /**
     * Signals a service delete event.
     */
    public void serviceDeletedEvent() {
        LOG.info("EVENT triggered: SRV_DELETED.");
        synchronized (this) {
            curEvent = EVENT.SRV_DELETED;
            transition();
        }
        action();
    }

    /**
     * Signals a service update event.
     */
    public void serviceUpdatedEvent() {
        LOG.info("EVENT triggered: SRV_UPDATED.");
        synchronized (this) {
            curEvent = EVENT.SRV_UPDATED;
            transition();
        }
        action();
    }

    /**
     * Signals a service initialized event.
     */
    public void serviceInitializedEvent() {
        LOG.info("EVENT triggered: SRV_INITIALIZED.");
        synchronized (this) {
            curEvent = EVENT.SRV_INITIALIZED;
            transition();
        }
        action();
    }

    /**
     * Signals a service not initialized event.
     */
    public void serviceNotInitializedEvent() {
        LOG.info("EVENT triggered: SRV_NOT_INITIALIZED.");
        synchronized (this) {
            curEvent = EVENT.SRV_NOT_INITIALIZED;
            transition();
        }
        action();
    }

    /**
     * Signals a service not running event.
     */
    public void serviceNotRunnningEvent() {
        LOG.info("EVENT triggered: SRV_NOT_RUNNING.");
        synchronized (this) {
            curEvent = EVENT.SRV_NOT_RUNNING;
            transition();
        }
        action();
    }

    /**
     * Signals a no service event.
     */
    public void serviceNoneEvent() {
        LOG.info("EVENT triggered: SRV_NONE.");
        synchronized (this) {
            curEvent = EVENT.SRV_NONE;
            transition();
        }
        action();
    }

    /**
     * Signals an errorEvent event.
     */
    public void errorEvent() {
        LOG.info("EVENT triggered: ERROR.");
        synchronized (this) {
            curEvent = EVENT.ERROR;
            transition();
        }
        action();
    }

    /**
     * Signals a container update event.
     */
    public void containerUpdatedEvent() {
        LOG.info("EVENT triggered: CON_UPDATED.");
        synchronized (this) {
            curEvent = EVENT.CON_UPDATED;
            transition();
        }
        action();
    }

    /**
     * Signals a container start initialization event.
     */
    public void containerInitEvent() {
        LOG.info("EVENT triggered: CON_INIT.");
        synchronized (this) {
            curEvent = EVENT.CON_INIT;
            transition();
        }
        action();
    }

    // -------------------------------- Setters -------------------------------- 
    public void setSrvMngr(ServiceManager srvMngr) {
        this.srvMngr = srvMngr;
    }

    public void setExecContainerInitCycle(Executable execContainerInitCycle) {
        this.execContainerInitCycle = execContainerInitCycle;
    }

    public void setExecContainerStartLifeCycle(Executable execContainerStartLifeCycle) {
        this.execContainerStartLifeCycle = execContainerStartLifeCycle;
    }

    public void setExecContainerUpdateLifeCycle(Executable execContainerUpdateLifeCycle) {
        this.execContainerUpdateLifeCycle = execContainerUpdateLifeCycle;
    }

    public void setExecContainerErrorLifeCycle(Executable execContainerErrorLifeCycle) {
        this.execContainerErrorLifeCycle = execContainerErrorLifeCycle;
    }

    public void setExecContainerBootCycle(Executable execContainerBootCycle) {
        this.execContainerBootCycle = execContainerBootCycle;
    }

    public void setExecContainerShutdownLifeCycle(Executable execContainerShutdownLifeCycle) {
        this.execContainerShutdownLifeCycle = execContainerShutdownLifeCycle;
    }
}
