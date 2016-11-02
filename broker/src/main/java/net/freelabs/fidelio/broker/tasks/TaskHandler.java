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
package net.freelabs.fidelio.broker.tasks;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that defines an executor for Tasks.
 */
public final class TaskHandler {

    /**
     * List of preStartTasks to execute.
     */
    private final List<Task> preStartTasks;
    /**
     * List of postStopTasks to execute.
     */
    private final List<Task> postStopTasks;
    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TaskHandler.class);

    /**
     * Constructor.
     *
     * @param preStartTasks pre start Tasks to execute.
     * @param postStopTasks post stop Tasks to execute.
     */
    public TaskHandler(List<Task> preStartTasks, List<Task> postStopTasks) {
        this.preStartTasks = preStartTasks;
        this.postStopTasks = postStopTasks;
    }

    /**
     * Constructor.
     */
    public TaskHandler() {
        preStartTasks = new ArrayList<>();
        postStopTasks = new ArrayList<>();
    }

    /**
     * Adds a task to the preStart task list.
     *
     * @param task a task to execute.
     */
    public void addPreStartTask(Task task) {
        preStartTasks.add(task);
    }

    /**
     * Adds a task to the postStop task list.
     *
     * @param task a task to execute.
     */
    public void addPostStopTask(Task task) {
        postStopTasks.add(task);
    }

    /**
     * Executes defined preStartTasks.
     */
    public void execPreStartTasks() {
        LOG.info("Executing pre-start tasks.");
        preStartTasks.stream().forEach((task) -> {
            task.run();
        });
    }

    /**
     * Executes defined postStopTasks.
     */
    public void execPostStopTasks() {
        LOG.info("Executing post-stop tasks.");
        postStopTasks.stream().forEach((task) -> {
            task.run();
        });
    }

}
