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
package net.freelabs.fidelio.core.cmd;

import java.util.ArrayList;
import java.util.List;
import net.freelabs.fidelio.core.boot.ProgramConf;

/**
 *
 * Class whose instances handle the execution of all system
 * {@link Command Command} objects.
 */
public final class CommandHandler implements Commandable {

    /**
     * Configuration of the program.
     */
    private final ProgramConf pConf;
    /**
     * Start command.
     */
    private final StartCmd startCmd;
    /**
     * Stop command.
     */
    private final StopCmd stopCmd;
    /**
     * Restart command.
     */
    private final RestartCmd restartCmd;
    /**
     * Delete command.
     */
    private final DeleteCmd deleteCmd;
    /**
     * List with names of the supported commands.
     */
    private final List<String> cmdNames;

    /**
     * Constructor.
     *
     * @param pConf
     */
    public CommandHandler(ProgramConf pConf) {
        this.pConf = pConf;
        // initialize commands
        startCmd = new StartCmd("start");
        stopCmd = new StopCmd("stop");
        restartCmd = new RestartCmd("restart");
        deleteCmd = new DeleteCmd("delete");

        // create list
        cmdNames = new ArrayList<>();
        // add to list
        cmdNames.add(startCmd.getCmdName());
        cmdNames.add(stopCmd.getCmdName());
        cmdNames.add(restartCmd.getCmdName());
        cmdNames.add(deleteCmd.getCmdName());

    }

    @Override
    public void exec_start(String... args) {
        startCmd.exec(pConf);
    }

    @Override
    public void exec_stop(String... args) {
        stopCmd.exec(pConf, args);
    }

    @Override
    public void exec_restart(String... args) {
        restartCmd.exec(pConf, args);
    }

    @Override
    public void exec_delete(String... args) {
        deleteCmd.exec(pConf, args);
    }

    /**
     *
     * @return the list with the names of the supported commands.
     */
    public List<String> getCmdNames() {
        return cmdNames;
    }

    /**
     *
     * @return the start Command object.
     */
    public StartCmd getStartCmd() {
        return startCmd;
    }

    /**
     *
     * @return the stop Command object.
     */
    public StopCmd getStopCmd() {
        return stopCmd;
    }

    /**
     * 
     * @return the restart Command object.
     */
    public RestartCmd getRestartCmd() {
        return restartCmd;
    }

    /**
     *
     * @return the clean Command object.
     */
    public DeleteCmd getDeleteCmd() {
        return deleteCmd;
    }
}
