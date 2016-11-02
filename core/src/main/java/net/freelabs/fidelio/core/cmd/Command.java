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

import net.freelabs.fidelio.core.boot.ProgramConf;

/**
 *
 * Class to be used as template to support implementations of user commands.
 */
abstract class Command {

    /**
     * The name of the user command.
     */
    protected String cmdName;

    /**
     * Constructor.
     *
     * @param cmd the name of the command.
     */
    public Command(String cmdName) {
        this.cmdName = cmdName;
    }

    /**
     * Executes the user command.
     */
    protected abstract void exec(ProgramConf pConf, String... args);

    /**
     * Exits program with error exit code and exit message.
     */
    protected abstract void errExit();

    /**
     *
     * @return the name of the command.
     */
    public String getCmdName() {
        return cmdName;
    }
}
