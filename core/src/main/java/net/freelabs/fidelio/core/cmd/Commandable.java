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

/**
 *
 * <p>
 * Interface that defines one method per command to be executed. For every
 * command one such method must be defined. A class instance that implements
 * this interface may handle the execution of all system commands.
 * <p>
 * Every method acts on a {@link Command Command} object using the {@link
 * ProgramConf ProgramConf} internally.
 * <p>
 * <b>Method naming convention</b>
 * <br>
 * A method that executes a command must be of void return type and its name
 * must start with prefix <b>'exec_'</b> followed by the <b>name of the
 * command</b>.
 * <br>
 * <b> Example</b> 
 * <br>
 * Method declaration for the stop command: public void exec_stop();
 */
public interface Commandable {

    public void exec_start(String... args);

    public void exec_stop(String... args);
    
    public void exec_restart(String... args);

    public void exec_delete(String... args);
    
}
