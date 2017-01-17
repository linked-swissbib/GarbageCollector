/*
 * Copyright (C) 2016 swissbib
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.swissbib.linked;


import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        for (Connector c : argParser(args)) {
            c.connect().execute().disconnect();
        }
    }

    private static List<Connector> argParser(String[] args) {

        String eshost;
        int esport;
        String esname;
        String esindex;


        Option oclean = Option.builder("clean")
                .argName("type")
                .desc("Clean type in index.")
                .build();
        Option ohelp = Option.builder("h")
                .longOpt("help")
                .desc("Help")
                .build();
        Option oeshost = Option.builder("eshost")
                .argName("host:port")
                .hasArg()
                .desc("hostname:port of Elasticsearch node.")
                .build();
        Option oesname = Option.builder("esname")
                .argName("cluster name")
                .hasArg()
                .desc("Name of Elasticsearch cluster.")
                .build();
        Option oesindex = Option.builder("esindex")
                .argName("index name")
                .hasArg()
                .desc("Name of Elasticsearch index.")
                .build();

        Options options = new Options();
        options.addOption(oclean)
                .addOption(ohelp)
                .addOption(oeshost)
                .addOption(oesname)
                .addOption(oesindex);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar garbageCollector.jar", options);

        List<Connector> al = new ArrayList<>();

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("eshost")) {
                eshost = cmd.getOptionValue("eshost").split(":")[0];
                esport = Integer.parseInt(cmd.getOptionValue("eshost").split(":")[1]);
            } else {
                eshost = "localhost";
                esport = 9300;
            }
            esname = (cmd.hasOption("esname")) ? cmd.getOptionValue("esname") : "elasticsearch";
            esindex = (cmd.hasOption("esindex")) ? cmd.getOptionValue("esindex") : "lsb";

            LocalSettings localSettings = new LocalSettings()
                    .setEsHost(eshost)
                    .setEsPort(esport)
                    .setEsCluster(esname)
                    .setEsIndex(esindex)
                    .setBulkSize(10000)
                    .setScrollSize(100)
                    .setScrollMinutes(2);

            if (cmd.hasOption("clean")) {
                // al.add(new WorkCleaner(localSettings));
                al.add(new PersonCleaner(localSettings));
                al.add(new OrganisationCleaner(localSettings));
            }

        } catch (ParseException e) {
            LOG.error(e.getMessage());
        }

        return al;
    }
}
