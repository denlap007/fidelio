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
package net.freelabs.fidelio.core.boot.cl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.CommaParameterSplitter;
import java.util.List;

/**
 *
 * Class whose instance defines all the command line parameters and commands for
 * the program.
 */
public class CliOptions {

    private String zkHosts;

    private int zkTimeout;

    private String dockerHost;

    private Boolean dockerTls;

    private String dockerCertPath;

    private String dockerConfigPath;

    private String dockerApiVer;

    private String dockerRegUrl;

    private String dockerRegUser;

    private String dockerRegPass;

    private String dockerRegMail;

    private static final int DOCKER_OPTS_NUM = 10;

    private static final int ZK_OPTS_NUM = 2;

    private static final String TOKEN_SPLITTER = "=";

    // --------------------------- Command Line Options ---------------------------
    @Parameter(names = {"-h", "--help"}, description = "Show this help message :).", help = true)
    private Boolean help;

    @Parameter(names = {"-v", "--version"}, description = "Show program version.")
    private Boolean version;

    @Parameter(names = {"-c", "--conf"}, description = "<program conf> Path to .properties file with program configuration.", required = false)
    private String conf;

    @Parameter(names = {"-z", "--zkOpts[]"}, description = "Zookeeper options "
            + "[hosts=<host1:port1,host2:port2...>,"
            + "timeout=<zk client session timeout>]", splitter = CommaParameterSplitter.class, required = false)
    private List<String> zkOptions;

    @Parameter(names = {"-d", "--dockerOpts[]"}, description = "Docker options "
            + "[host=<tcp://ip or unix:///socket>,"
            + "tls=<true to enable https>,"
            + "cert=<path to certs for tls>,"
            + "config=<path to config like .dockercfg>,"
            + "api=<api version>,"
            + "regUrl=<registry url>,"
            + "regUser=<regisrty username>,"
            + "regPass=<registry password>,"
            + "regEmail=<registry email>]", splitter = CommaParameterSplitter.class, required = false)
    private List<String> dockerOptions;

    @Parameter(names = {"-l", "--log"}, description = "<log4j properties> Path to log4j.properties file with log configuration.", required = false)
    private String log4j;

    // --------------------------- Commands ---------------------------
    // start command
    @Parameters(commandDescription = "Start application deployment.")
    public class StartCmdOpt {

        @Parameter(names = {"-h", "--help"}, description = "Help for start command.", help = true)
        private Boolean help;

        @Parameter(names = {"-s", "--xmlSchema"}, description = "<schema file> Path to xml schema file.", required = false)
        private String schema;

        @Parameter(names = {"-x", "--xmlFile"}, description = "<app xml file> Path to application description xml file.", required = true)
        private String xml;

        // Getters
        public boolean isHelp() {
            if (help == null) {
                help = false;
            }
            return this.help;
        }

        public String getSchema() {
            return schema;
        }

        public String getXml() {
            return xml;
        }
    }

    // stop command
    @Parameters(commandDescription = "Stop deployed application.")
    public class StopCmdOpt {

        @Parameter(names = {"-h", "--help"}, description = "Help for stop command.", help = true)
        private Boolean help;

        @Parameter(description = "<app id> The id of the deployed application to stop.", required = true)
        private List<String> args;

        // Getters
        public boolean isHelp() {
            if (help == null) {
                help = false;
            }
            return this.help;
        }

        public List<String> getArgs() {
            return args;
        }
    }

    // restart command
    @Parameters(commandDescription = "Restart deployed application.")
    public class RestartCmdOpt {

        @Parameter(names = {"-h", "--help"}, description = "Help for restart command.", help = true)
        private Boolean help;

        @Parameter(description = "<app id> The id of the deployed application to restart.", required = true)
        private List<String> args;

        // Getters
        public boolean isHelp() {
            if (help == null) {
                help = false;
            }
            return this.help;
        }

        public List<String> getArgs() {
            return args;
        }
    }

    // delete command
    @Parameters(commandDescription = "Delete deployed application (zookeeper namespace and docker containers).")
    public class DeleteCmdOpt {

        @Parameter(names = {"-h", "--help"}, description = "Help for delete command.", help = true)
        private Boolean help;

        @Parameter(description = "<app id> The id of the deployed application to delete.", required = true)
        private List<String> args;

        // Getters
        public boolean isHelp() {
            if (help == null) {
                help = false;
            }
            return this.help;
        }

        public List<String> getArgs() {
            return args;
        }
    }

    // --------------------------- Processing ---------------------------
    public boolean parseZkOpts() {
        boolean parsedOptions = true;
        if (zkOptions.size() > ZK_OPTS_NUM) {
            System.err.println("ERROR: Too many zookeeper options defined. See \'fidelio --help\'.");
        } else if (!zkOptions.isEmpty()) {
            for (int i = 0; i < zkOptions.size(); i++) {
                boolean parsed = parseAndSetZkOption(zkOptions.get(i));
                if (!parsed) {
                    parsedOptions = false;
                    System.err.println("ERROR: Invalid zookeeper option: "
                            + zkOptions.get(i)
                            + ". See \'fidelio --help\'.");
                    break;
                }
            }
        } else {
            System.err.println("ERROR: No zookeeper option defined. See \'fidelio --help\'.");
        }

        return parsedOptions;
    }

    /**
     * Parses a zookeeper option, checks its validity and sets its value.
     *
     * @param kvPair the key=value pair, defining a zookeeper option and its
     * value.
     * @return true if option parsed, validated and set successfully.
     */
    private boolean parseAndSetZkOption(String kvPair) {
        String value = checkKVPair(kvPair);

        if (value != null) {
            String[] kv = kvPair.split(TOKEN_SPLITTER);
            boolean set = setZkrOption(kv[0], kv[1]);
            if (!set) {
                value = null;
            }
        }

        return value != null;
    }

    /**
     * Checks for a match for the key and then sets the value to the
     * corresponding docker option.
     *
     * @param key the key to match with a zookeeper option.
     * @param value the value of the zookeeper option to set.
     * @return true if value set to zookeeper option mapped by the key.
     */
    private boolean setZkrOption(String key, String value) {
        boolean set = true;
        switch (key.toLowerCase()) {
            case "timeout":
                try {
                    int timeout = Integer.parseInt(value);
                    if (timeout > 0) {
                        zkTimeout = timeout;
                    } else {
                        set = false;
                    }
                } catch (NumberFormatException ex) {
                    set = false;
                }
                break;
            case "hosts":
                zkHosts = value;
                break;
            default:
                set = false;
                break;
        }
        return set;
    }

    /**
     * Parses docker options list returned by the command line parser.
     *
     * @return true if parsed successfully.
     */
    public boolean parseDockerOpts() {
        boolean parsedOptions = true;
        if (dockerOptions.size() > DOCKER_OPTS_NUM) {
            System.err.println("ERROR: Too many docker options defined. See \'fidelio --help\'.");
        } else if (!dockerOptions.isEmpty()) {
            for (int i = 0; i < dockerOptions.size(); i++) {
                boolean parsed = parseAndSetDockerOption(dockerOptions.get(i));
                if (!parsed) {
                    parsedOptions = false;
                    System.err.println("ERROR: Invalid docker option: "
                            + dockerOptions.get(i)
                            + ". See \'fidelio --help\'.");
                    break;
                }
            }
        } else {
            System.err.println("ERROR: No docker option defined. See \'fidelio --help\'.");
        }
        return parsedOptions;
    }

    /**
     * Parses a docker option, checks its validity and sets its value.
     *
     * @param kvPair the key=value pair, defining a docker option and its value.
     * @return true if option parsed, validated and set successfully.
     */
    private boolean parseAndSetDockerOption(String kvPair) {
        String value = checkKVPair(kvPair);

        if (value != null) {
            String[] kv = kvPair.split(TOKEN_SPLITTER);
            boolean set = setDockerOption(kv[0], kv[1]);
            if (!set) {
                value = null;
            }
        }

        return value != null;
    }

    /**
     * Checks the key=value pair specified for validity. The pair is valid if
     * and only if it complies to the following format: key=value. If the pair
     * is valid then it is returned else null is returned.
     *
     * @param kvPair the key=value pair to check.
     * @return the kv pair if valid or else null.
     */
    private String checkKVPair(String kvPair) {
        String value = null;
        String[] splitElem = kvPair.split(TOKEN_SPLITTER);
        if (splitElem.length == 2) {
            value = kvPair;
        }
        return value;
    }

    /**
     * Checks for a match for the key and then sets the value to the
     * corresponding docker option.
     *
     * @param key the key to match with a docker option.
     * @param value the value of the docker option to set.
     * @return true if value set to docker option mapped by the key.
     */
    private boolean setDockerOption(String key, String value) {
        boolean set = true;
        switch (key.toLowerCase()) {
            case "host":
                dockerHost = value;
                break;
            case "tls":
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    dockerTls = Boolean.parseBoolean(value);
                } else {
                    set = false;
                }
                break;
            case "cert":
                dockerCertPath = value;
                break;
            case "config":
                dockerConfigPath = value;
                break;
            case "api":
                dockerApiVer = value;
                break;
            case "regurl":
                dockerRegUrl = value;
                break;
            case "reguser":
                dockerRegUser = value;
                break;
            case "regpass":
                dockerRegPass = value;
                break;
            case "regmail":
                dockerRegMail = value;
                break;
            default:
                set = false;
                break;
        }
        return set;
    }

    // ---------------------- Command Line Options Getters ----------------------
    public boolean isVersion() {
        if (version == null) {
            version = false;
        }
        return version;
    }

    public boolean isHelp() {
        if (help == null) {
            help = false;
        }
        return help;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getLog4j() {
        return log4j;
    }

    public void setLog4j(String log4j) {
        this.log4j = log4j;
    }

    public String getZkHosts() {
        return zkHosts;
    }

    public int getZkTimeout() {
        return zkTimeout;
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public Boolean getDockerTls() {
        return dockerTls;
    }

    public String getDockerCertPath() {
        return dockerCertPath;
    }

    public String getDockerConfigPath() {
        return dockerConfigPath;
    }

    public String getDockerApiVer() {
        return dockerApiVer;
    }

    public String getDockerRegUrl() {
        return dockerRegUrl;
    }

    public String getDockerRegUser() {
        return dockerRegUser;
    }

    public String getDockerRegPass() {
        return dockerRegPass;
    }

    public String getDockerRegMail() {
        return dockerRegMail;
    }

    public List<String> getZkOptions() {
        return zkOptions;
    }

    public List<String> getDockerOptions() {
        return dockerOptions;
    }

}
