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
import net.freelabs.fidelio.core.schema.Tasks;

/**
 *
 * <p>
 Template class whose instances may create preStartTasks.
 <p>
 * For every task a create method must be defined. The implementation of the
 * create method should be done in sub-class instances of this class.
 * <p>
 * The
 * {@link #createTasks(net.freelabs.fidelio.core.generated.Tasks) createTasks}
 method executes all methods that create preStartTasks. Every added create method for
 a new task must be executed inside this method and the new Task instance must
 be added to the Task list, that holds all the Task objects.
 */
public abstract class TaskCreator {

    /**
     * List of preStartTasks (objects of type {@link Task Task})to be executed before
     * program start section.
     */
    protected List<Task> preStartTasks;

    /**
     * List of postStopTasks (objects of type {@link Task Task})to be executed after 
     * program stop section.
     */
    protected List<Task> postStopTasks;

    /**
     * Creates all the tasks.
     *
     * @param taskRes the resources of {@link Tasks Tasks} declared on
     * application description.
     */
    protected final void createTasks(Tasks taskRes) {
        // create list for preStartTasks
        preStartTasks = new ArrayList<>();
        postStopTasks = new ArrayList<>();

        // ALL CREATE METHODS MUST BE CALLED HERE
        // create substEnv task
        Task substEnv = createSubstEnvTask();
        Task restoreFiles = createRestoreTask();

        // ALL CREATED TASKS MUST BE ADDED TO THE LIST OF TASKS
        preStartTasks.add(substEnv);
        
        postStopTasks.add(restoreFiles);
    }

    /**
     * Creates the substEnv task, that expands environment variables to
     * configuration files. environment variables.
     *
     * @return a task for execution.
     */
    protected abstract Task createSubstEnvTask();
    /**
     * Restores the original files where environment substitution has been applied.
     * @return a task for execution.
     */
    protected abstract Task createRestoreTask();

}
