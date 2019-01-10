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
import java.util.HashMap;
import java.util.HashSet;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        LocalSettings localSettings = argParser(args);

        EsClient client = new EsClient(localSettings);

        LOG.info("Fetching all identifiers in `dct:contributor` fields in type `bibliographicResource`");
        HashMap<String, HashSet<String>> contribIds =
                new ContributorFetcher(client).execute(localSettings.getEsIndex());
        LOG.info("Found {} unique `person` and {} `organisation` identifiers",
                contribIds.get("pers").size(), contribIds.get("orga").size());

        IdFetcher idFetcher = new IdFetcher(client);
        Cleaner cleaner = new Cleaner(client);

        LOG.info("Fetching identifiers of all `person` documents");
        ArrayList<String> removedPers =
                idFetcher.execute(localSettings.getEsIndex(), "person", contribIds.get("pers"));
        LOG.info("Found {} obsolete `person` documents", removedPers);
        LOG.info("Remove obsolete `person` documents in index");
        cleaner.execute(localSettings.getEsIndex(), "person", removedPers);
        LOG.info("Checking `organisation` documents if obsolete");
        ArrayList<String> removedOrga =
                idFetcher.execute(localSettings.getEsIndex(), "organisation", contribIds.get("orga"));
        LOG.info("Found {} obsolete `organisation` documents", removedOrga);
        LOG.info("Remove obsolete `organisation` documents in index");
        cleaner.execute(localSettings.getEsIndex(), "organisation", removedOrga);

        LOG.info("All finished!");

        client.disconnect();
    }

    private static LocalSettings argParser(String[] args) {
        Option ohelp = Option.builder("h")
                .longOpt("help")
                .desc("Help")
                .build();
        Option oeshost = Option.builder("eshost")
                .argName("host:port")
                .hasArg()
                .desc("hostname:port of Elasticsearch node.")
                .required()
                .build();
        Option oesname = Option.builder("esname")
                .argName("cluster name")
                .hasArg()
                .desc("Name of Elasticsearch cluster.")
                .required()
                .build();
        Option oesindex = Option.builder("esindex")
                .argName("index name")
                .hasArg()
                .desc("Name of Elasticsearch index.")
                .required()
                .build();

        Options options = new Options();
        options.addOption(ohelp)
                .addOption(oeshost)
                .addOption(oesname)
                .addOption(oesindex);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar garbageCollector.jar", options);

        CommandLineParser parser = new DefaultParser();
        LocalSettings localSettings = new LocalSettings();
        try {
            CommandLine cmd = parser.parse(options, args);

            localSettings.setEsHost(cmd.getOptionValue("eshost").split(":")[0])
                    .setEsPort(Integer.parseInt(cmd.getOptionValue("eshost").split(":")[1]))
                    .setEsCluster(cmd.getOptionValue("esname"))
                    .setEsIndex(cmd.getOptionValue("esindex"));

        } catch (ParseException e) {
            LOG.error(e.getMessage());
            System.exit(1);
        }

        return localSettings;
    }
}
