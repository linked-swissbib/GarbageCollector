package org.swissbib.linked;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.existsQuery;

class ContributorFetcher {

    private final static Logger LOG = LoggerFactory.getLogger(ContributorFetcher.class);

    private EsClient esClient;

    ContributorFetcher(EsClient esClient) {
        this.esClient = esClient;
    }

    HashMap<String, HashSet<String>> execute(String index) {
        HashMap<String, HashSet<String>> results = new HashMap<>();
        HashSet<String> pers = new HashSet<>();
        HashSet<String> orga = new HashSet<>();
        results.put("pers", pers);
        results.put("orga", orga);
        SearchResponse scrollResp = esClient.client
                .prepareSearch(index)
                .addSort("_doc", SortOrder.ASC)
                .setTypes("bibliographicResource")
                .setFetchSource("dct:contributor", null)
                .setScroll(new TimeValue(60000))
                .setQuery(existsQuery("dct:contributor"))
                .setSize(1000)
                .get();
        do {
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                String rawContributors = hit.getSource().get("dct:contributor").toString();
                ArrayList<String> contributors = new ArrayList<>();
                if (rawContributors.startsWith("[")) {
                    contributors.addAll(Arrays.asList(rawContributors.substring(1, rawContributors.length() - 1).split(", ")));
                } else {
                    contributors.add(rawContributors);
                }
                for (String contributor : contributors) {
                    List<String> splitted = Arrays.asList(contributor.split("/"));
                    String id = splitted.get(splitted.size() - 1);
                    if (splitted.get(splitted.size() - 2).equals("person")) {
                        pers.add(id);
                    } else {
                        orga.add(id);
                    }
                }
            }
            scrollResp = esClient.client
                    .prepareSearchScroll(scrollResp.getScrollId())
                    .setScroll(new TimeValue(60000))
                    .execute()
                    .actionGet();
        } while (scrollResp.getHits().getHits().length != 0);
        return results;
    }
}
