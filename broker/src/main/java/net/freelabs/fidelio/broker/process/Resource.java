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
package net.freelabs.fidelio.broker.process;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Class that represents a resource to be executed.
 *
 */
public final class Resource {

    /**
     * A resource to execute.
     */
    private String res;
    /**
     * Abort all remaining resource execution if resource execution fails.
     */
    private final boolean abortOnFail;

    /**
     * Constructor.
     *
     * @param res the resource.
     * @param abortOnFail Abort all descending processes execution if resource
     * execution fails.
     */
    public Resource(String res, boolean abortOnFail) {
        this.res = res;
        this.abortOnFail = abortOnFail;
    }

    /**
     * <p>
     * Returns the command and arguments specified in the resource.
     * <p>
     * The method uses a regular expression to match and split tokens using
     * space delimeter and treats tokens surrounded by quotes (double or single)
     * as one.
     * <p>
     * Essentially, we want to grab two kinds of things from the resource:
     * "sequences of characters that aren't spaces or quotes, and sequences of
     * characters that begin and end with a quote, with no quotes in between,
     * for two kinds of quotes".
     * <p>
     * The capturing groups were added because we don't want the quotes in the
     * list.
     *
     * @return the command and arguments that are specified in the resource.
     */
    public List<String> getResCmdArgs() {
        List<String> cmdArgs = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(res);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                cmdArgs.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                cmdArgs.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                cmdArgs.add(regexMatcher.group());
            }
        }
        return cmdArgs;
    }

    // Getters - Setters
    /**
     *
     * @return the resource.
     */
    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    /**
     *
     * @return the resource description.
     */
    public String getDescription() {
        return res;
    }

    public boolean isAbortOnFail() {
        return abortOnFail;
    }

}
