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

import java.util.List;
import java.util.Map;
import net.freelabs.fidelio.core.schema.Tasks;

/**
 *
 * Class whose instances map the resources of {@link Tasks Tasks} declared on
 * application description to objects of type {@link Task Task} to be executed.
 */
public final class TaskMapper extends TaskCreator {

    /**
     * The environment of the container processes.
     */
    private final Map<String, String> env;
    /**
     * The resources of {@link Tasks Tasks} declared on application description.
     */
    private final Tasks taskResources;

    /**
     * Constructor.
     *
     * @param taskRes the resources of {@link Tasks Tasks} declared on
     * application description.
     * @param env the environment of the container processes.
     */
    public TaskMapper(Tasks taskRes, Map<String, String> env) {
        this.env = env;
        this.taskResources = taskRes;
        // create preStartTasks for execution
        createTasks(taskRes);
    }

    @Override
    protected Task createSubstEnvTask() {
        return  (new SubstEnvTask(taskResources.getSubstEnv(), env));
    }

    /**
     *
     * @return the list with the preStartTasks to be executed.
     */
    public List<Task> getPreStartTasks() {
        return preStartTasks;
    }
    
        /**
     *
     * @return the list with the preStartTasks to be executed.
     */
    public List<Task> getPostStopTasks() {
        return postStopTasks;
    }

    @Override
    protected Task createRestoreTask() {
        return  (new RestoreFilesTask(taskResources.getSubstEnv()));
    }
}
