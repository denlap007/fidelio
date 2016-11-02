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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.freelabs.fidelio.broker.BrokerConf;
import net.freelabs.fidelio.core.schema.SubstEnv;
import net.freelabs.fidelio.core.schema.SubstEnvElem;
import org.slf4j.LoggerFactory;

/**
 *
 * Class whose instances implement the task of restoring files to which
 * environment substitution has been applied.
 */
final class RestoreFilesTask implements Task {

    /**
     * Tag from application description containing all the files to apply
     * environment variables substitution.
     */
    private final SubstEnv substEnv;
    /**
     * A Logger object.
     */
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RestoreFilesTask.class);

    /**
     * Constructor
     *
     * @param substEnv tag from application description containing all the files
     * to apply environment variables substitution.
     */
    public RestoreFilesTask(SubstEnv substEnv) {
        this.substEnv = substEnv;
    }

    @Override
    public void run() {
        List<SubstEnvElem> substEnvElems = substEnv.getFilePath();

        Map<String, File> origFilesMap = new HashMap<>();
        // get original files as declared in substEnv tag
        for (SubstEnvElem elem : substEnvElems) {
            File file = new File(elem.getValue());
            String name = file.getName();
            origFilesMap.put(name, file);
        }

        // get all files in program's restore filder
        File restoreDir = new File(BrokerConf.RESTORE_DIR);
        // create filter to get only declared files from restore folder
        FileFilter filter = (File pathname) -> origFilesMap.containsKey(pathname.getName());
        // get files
        File[] filesToRestore = restoreDir.listFiles(filter);

        for (File fileToRestore : filesToRestore) {
            // get the name of the file to restore
            String nameOfRestFile = fileToRestore.getName();
            // get the source path for copy
            Path src = fileToRestore.toPath();
            // get the original file
            File origFile = origFilesMap.get(nameOfRestFile);
            // get the destination path for copy
            Path dst = origFile.toPath();
            // restore file
            try {
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(src);
                LOG.info("Restored file: {}", dst);
            } catch (IOException ex) {
                LOG.error("FAILED to restore file: {}. {}", src, ex.getMessage());
            }
        }
    }

}
