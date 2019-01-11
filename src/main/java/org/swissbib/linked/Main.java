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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        LocalSettings localSettings = argParser(args);

        EsClient client = new EsClient(localSettings);

        if (localSettings.getDryRun()) {
            LOG.info("Do dry run (no deletions)");
        }

        LOG.info("Fetching all identifiers in `dct:contributor` fields in type `bibliographicResource`");
        HashMap<String, HashSet<String>> contribIds =
                new ContributorFetcher(client).execute(localSettings.getEsIndex());
        LOG.info("Found {} unique `person` and {} `organisation` identifiers",
                contribIds.get("pers").size(), contribIds.get("orga").size());


        IdFetcher idFetcher = new IdFetcher(client);
        Cleaner cleaner = new Cleaner(client);

        LOG.info("Checking `person` documents if obsolete");
        ArrayList<String> removedPers =
                idFetcher.execute(localSettings.getEsIndex(), "person", contribIds.get("pers"));
        LOG.info("Found {} obsolete `person` documents", removedPers.size());
        if (!localSettings.getDryRun()) {
            LOG.info("Remove obsolete `person` documents in index");
            cleaner.execute(localSettings.getEsIndex(), "person", removedPers);
        }

        LOG.info("Checking `organisation` documents if obsolete");
        ArrayList<String> removedOrga =
                idFetcher.execute(localSettings.getEsIndex(), "organisation", contribIds.get("orga"));
        LOG.info("Found {} obsolete `organisation` documents", removedOrga.size());
        if (!localSettings.getDryRun()) {
            LOG.info("Remove obsolete `organisation` documents in index");
            cleaner.execute(localSettings.getEsIndex(), "organisation", removedOrga);
        }

        if (localSettings.getLogPath().length() > 0) {
            writeListToFile(localSettings.getLogPath());
        }

        LOG.info("All finished!");

        client.disconnect();
    }

    private static LocalSettings argParser(String[] args) {
        Option ohelp = Option.builder("h")
                .longOpt("help")
                .required(false)
                .desc("Help")
                .build();
        Option oeshost = Option.builder("u")
                .argName("host:port")
                .hasArg(true)
                .longOpt("url")
                .desc("hostname:port of Elasticsearch node")
                .required(true)
                .build();
        Option oesname = Option.builder("c")
                .argName("cluster name")
                .hasArg(true)
                .longOpt("cluster")
                .desc("Name of Elasticsearch cluster")
                .required(true)
                .build();
        Option oesindex = Option.builder("i")
                .argName("index")
                .hasArg(true)
                .longOpt("index")
                .desc("Name of Elasticsearch index")
                .required(true)
                .build();
        Option odryrun = Option.builder("d")
                .desc("Do dry run (no deletions)")
                .longOpt("dry-run")
                .hasArg(false)
                .required(false)
                .build();
        Option ologpath = Option.builder("l")
                .desc("Path to transaction log file")
                .longOpt("log")
                .hasArg(true)
                .required(false)
                .build();

        Options options = new Options();
        options.addOption(ohelp)
                .addOption(oeshost)
                .addOption(oesname)
                .addOption(oesindex)
                .addOption(odryrun)
                .addOption(ologpath);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar garbageCollector.jar", options);

        CommandLineParser parser = new DefaultParser();
        LocalSettings localSettings = new LocalSettings();
        try {
            CommandLine cmd = parser.parse(options, args);

            localSettings.setEsHost(cmd.getOptionValue("u").split(":")[0])
                    .setEsPort(Integer.parseInt(cmd.getOptionValue("u").split(":")[1]))
                    .setEsCluster(cmd.getOptionValue("c"))
                    .setEsIndex(cmd.getOptionValue("i"))
                    .setDryRun(cmd.hasOption("d"))
                    .setLogPath(cmd.hasOption("l") ? cmd.getOptionValue("l") : "");

        } catch (ParseException e) {
            LOG.error(e.getMessage());
            System.exit(1);
        }

        return localSettings;
    }

    @SafeVarargs
    private static void writeListToFile(String filepath, List<String>... lists) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath))) {
            for (List<String> list : lists) {
                for (String line : list) {
                    bw.write(line + "\n");
                }
            }
            String content = "This is the content to write into file\n";
            bw.write(content);
            System.out.println("Done");
        } catch (IOException e) {
            LOG.error("Can't write to file {}.\nError: {}", filepath, e.getMessage());
        }
    }
}
