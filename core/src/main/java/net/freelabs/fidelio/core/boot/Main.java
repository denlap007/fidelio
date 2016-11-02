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
package net.freelabs.fidelio.core.boot;

import net.freelabs.fidelio.core.cmd.CommandHandler;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import net.freelabs.fidelio.core.boot.cl.CliOptions;
import net.freelabs.fidelio.core.boot.cl.CliOptions.StartCmdOpt;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that holds the main method of the program. Parses command line
 * arguments and boots the program.
 */
public final class Main {

    /**
     * A Logger object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /**
     * Main.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        /* FOR TESTING
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();
        String[] inputArgs = input.split(" ");
        args = inputArgs;
        */
        //-------------------------------------------------------------------- 

        // create object that holds program's configuration
        ProgramConf pConf = new ProgramConf();
        // create commnad handler to execute commands
        CommandHandler cmdExec = new CommandHandler(pConf);
        // create object with supported command line parameters and commands
        CliOptions opts = new CliOptions();
        // create the command line parser, initialize with cli options-cmds defined
        JCommander cl = new JCommander(opts);
        cl.setProgramName(ProgramConf.getPROGRAM_NAME());
        cl.setCaseSensitiveOptions(false);
        cl.setAllowAbbreviatedOptions(true);

        /* COMMANDS
         instantiate inner classes of CliOptions (commands) */
        CliOptions.StartCmdOpt startCmdOpt = opts.new StartCmdOpt();
        CliOptions.StopCmdOpt stopCmdOpt = opts.new StopCmdOpt();
        CliOptions.RestartCmdOpt restartCmdOpt = opts.new RestartCmdOpt();
        CliOptions.DeleteCmdOpt deleteCmdOpt = opts.new DeleteCmdOpt();
        // get command names
        String start = cmdExec.getStartCmd().getCmdName();
        String stop = cmdExec.getStopCmd().getCmdName();
        String restart = cmdExec.getRestartCmd().getCmdName();
        String delete = cmdExec.getDeleteCmd().getCmdName();
        // add commands defined to parser
        cl.addCommand(start, startCmdOpt);
        cl.addCommand(stop, stopCmdOpt);
        cl.addCommand(restart, restartCmdOpt);
        cl.addCommand(delete, deleteCmdOpt);

        // parse cli arguments
        try {
            if (args.length == 0) {
                cl.parse("");
            }
            cl.parse(args);
        } catch (ParameterException e) {
            // show error msg
            System.err.print(e.getMessage() + ". See \'fidelio --help\'.\n");
            // exit
            errExit();
        }
        // get the command, if any, entered by the user
        String parsedCmd = cl.getParsedCommand();

        if (opts.getDockerOptions() != null) {
            // parse docker and options
            boolean parsed = opts.parseDockerOpts();
            if (!parsed) {
                // print usage
                errExit();
            }
        }
        if (opts.getZkOptions() != null) {
            boolean parsed = opts.parseZkOpts();
            if (!parsed) {
                // print usage
                errExit();
            }
        }

        boolean error = true;

        if (opts.isHelp()) {
            // program help
            cl.usage();
            error = false;
        } else if (opts.isVersion()) {
            // program version
            System.out.println("Maestro  v" + ProgramConf.getVERSION());
            error = false;
        } else if (parsedCmd.equals(start)) {
            // start command
            if (startCmdOpt.isHelp()) {
                cl.usage(start);
            } else if (programConfExists(pConf, opts)) {
                // load cli options
                loadProgramCliOpts(pConf, opts);
                // load start cli options
                loadStartCmdOpts(pConf, startCmdOpt);
                // load unset options from file, if any
                if (loadFromFileUnsetOpts(pConf, opts)) {
                    // load log4j properties
                    if (loadLod4jProperties(pConf.getLog4jPropertiesPath())) {
                        // check if program configuration is complete
                        if (pConf.isConfInitForStart()) {
                            error = false;
                            // execute START command
                            cmdExec.exec_start();
                        } else {
                            LOG.error("Program configuration NOT initialized. Check the .properties file and/or user input.");
                        }
                    }
                }
            }
        } else if (parsedCmd.equals(stop)) {
            // stop command
            if (stopCmdOpt.isHelp()) {
                cl.usage(stop);
            } else if (programConfExists(pConf, opts)) {
                // load cli options
                loadProgramCliOpts(pConf, opts);
                // load unset options from file, if any
                if (loadFromFileUnsetOpts(pConf, opts)) {
                    // load log4j properties
                    if (loadLod4jProperties(pConf.getLog4jPropertiesPath())) {
                        // check if program configuration is complete
                        if (pConf.isConfInitForOtherCommands()) {
                            error = false;
                            // execute STOP command
                            cmdExec.exec_stop(stopCmdOpt.getArgs().get(0));
                        } else {
                            LOG.error("Program configuration NOT initialized. Check the .properties file and/or user input.");
                        }
                    }
                }
            }
        } else if (parsedCmd.equals(restart)) {
            // restart command
            if (restartCmdOpt.isHelp()) {
                cl.usage(restart);
            } else if (programConfExists(pConf, opts)) {
                // load cli options
                loadProgramCliOpts(pConf, opts);
                // load unset options from file, if any
                if (loadFromFileUnsetOpts(pConf, opts)) {
                    // load log4j properties
                    if (loadLod4jProperties(pConf.getLog4jPropertiesPath())) {
                        // check if program configuration is complete
                        if (pConf.isConfInitForOtherCommands()) {
                            error = false;
                            // execute RESTART command
                            cmdExec.exec_restart(restartCmdOpt.getArgs().get(0));
                        } else {
                            LOG.error("Program configuration NOT initialized. Check the .properties file and/or user input.");
                        }
                    }
                }
            }
        } else if (parsedCmd.equals(delete)) {
            // delete command
            if (deleteCmdOpt.isHelp()) {
                cl.usage(delete);
            } else if (programConfExists(pConf, opts)) {
                // load cli options
                loadProgramCliOpts(pConf, opts);
                // load unset options from file, if any
                if (loadFromFileUnsetOpts(pConf, opts)) {
                    // load log4j properties
                    if (loadLod4jProperties(pConf.getLog4jPropertiesPath())) {
                        // check if program configuration is complete
                        if (pConf.isConfInitForOtherCommands()) {
                            error = false;
                            // execute DELETE command
                            cmdExec.exec_delete(deleteCmdOpt.getArgs().get(0));
                        } else {
                            LOG.error("Program configuration NOT initialized. Check the .properties file and/or user input.");
                        }
                    }
                }
            }
        }

        if (error) {
            errExit();
        }
    }

    private static boolean programConfExists(ProgramConf pConf, CliOptions opts) {
        boolean exists = true;
        if (opts.getConf() == null) {
            // check for program's propetries file
            if (pConf.isProgramPropertiesInRunningDir() == false) {
                System.err.println("ERROR: Cannot locate fidelio.properties file. "
                        + "Place the file in your running dir, or "
                        + "specify a custom path with -c command line option. "
                        + "See \'fidelio --help\'.");
                exists = false;
            }
        }
        return exists;
    }

    private static boolean loadFromFileUnsetOpts(ProgramConf pConf, CliOptions opts) {
        boolean loaded;
        // if custom path for program configuration is set load values
        // only those not set by the uer
        if (opts.getConf() != null) {
            loaded = pConf.loadFromFileUnset(opts.getConf());
        } else {
            // load from current running path, if exists, options not set by user
            loaded = pConf.loadFromFileUnset("");
        }
        return loaded;
    }

    private static void loadProgramCliOpts(ProgramConf pConf, CliOptions opts) {
        // set (if specified) cli options before commands
        pConf.setZkHosts(opts.getZkHosts());
        pConf.setZkSessionTimeout(opts.getZkTimeout());
        // docker
        pConf.setDockerHost(opts.getDockerHost());
        pConf.setDockerTlsVerify(opts.getDockerTls());
        pConf.setDockerCertPath(opts.getDockerCertPath());
        pConf.setDockerConfigPath(opts.getDockerConfigPath());
        pConf.setDockerApiVersion(opts.getDockerApiVer());
        pConf.setDockerRegistryUrl(opts.getDockerRegUrl());
        pConf.setDockerRegistryUser(opts.getDockerRegUser());
        pConf.setDockerRegistryPass(opts.getDockerRegPass());
        pConf.setDockerRegistryMail(opts.getDockerRegMail());
        // log
        pConf.setLog4jPropertiesPath(opts.getLog4j());
    }

    private static void loadStartCmdOpts(ProgramConf pConf, StartCmdOpt startCmdOpt) {
        // xml
        pConf.setXmlSchemaPath(startCmdOpt.getSchema());
        pConf.setXmlFilePath(startCmdOpt.getXml());
    }

    /**
     * Exits program due to error using an error code.
     */
    private static void errExit() {
        System.exit(-1);
    }

    /**
     * Loads the log4j properties file.
     *
     * @param file the path of the log4j properties file.
     */
    private static boolean loadLod4jProperties(String file) {
        boolean loaded = false;
        if (file == null) {
            file = "";
        }
        // load from file on disk
        Properties logProperties = new Properties();
        try {
            logProperties.load(new FileInputStream(file));
            PropertyConfigurator.configure(logProperties);
            loaded = true;
            LOG.debug("Loaded log4j properties file: {}", file);
        } catch (IOException ex) {
            if (file.isEmpty()) {
                System.out.println("WARNING: No log4j.properties file specified. Checking running directory.");
            } else {
                System.out.println("WARNING: Could not load log4j.properties file: "
                        + file
                        + ". Checking running directory. " + ex.getMessage());
            }
        }

        // load from running dir
        if (!loaded) {
            try {
                file = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                if (file != null) {
                    String runningFolder = new File(file).getParent();
                    file = runningFolder + File.separator + "log4j.properties";
                    logProperties.load(new FileInputStream(file));
                    PropertyConfigurator.configure(logProperties);
                    LOG.info("Loaded log4j.properties file: {}", file);
                    loaded = true;
                }
            } catch (IOException | URISyntaxException ex1) {
                System.out.println("WARNING: Could not load log4j.properties file from running directory: "
                        + ex1.getMessage()
                        + ". Checking classpath.");
            }
        }

        // load from classpath
        if (!loaded) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL url = loader.getResource("log4j.properties");
            if (url != null) {
                PropertyConfigurator.configure(url);
                loaded = true;
                LOG.info("Loaded log4j.properties file: {}", url.toString());
            } else {
                System.out.println("ERROR: FAILED to locate a valid log4j.properties file. "
                        + "Put the file in your running dirextory, classpath or declare its "
                        + "path to program's .properties file or through command line!");
            }
        }
        return loaded;
    }

}
