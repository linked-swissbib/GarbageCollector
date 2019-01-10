package org.swissbib.linked;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.existsQuery;

class ContributorFetcher {

    private final static Logger LOG = LoggerFactory.getLogger(ContributorFetcher.class);

    private EsClient esClient;

    ContributorFetcher(EsClient esClient) {
        this.esClient = esClient;
    }

    HashMap<String, HashSet<String>> execute(String index) {
        SearchResponse scrollResp = esClient.client
                .prepareSearch(index)
                .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                .setTypes("bibliographicResource")
                .setFetchSource("dct:contributor", null)
                .setScroll(new TimeValue(60000))
                .setQuery(existsQuery("dct:contributor"))
                .setSize(1000)
                .get();
        HashMap<String, HashSet<String>> results = new HashMap<>();
        HashSet<String> pers = new HashSet<>();
        HashSet<String> orga = new HashSet<>();
        results.put("pers", pers);
        results.put("orga", orga);
        do {
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                for (Object contributor : hit.getField("dct:contributor").getValues()) {
                    List<String> splitted = Arrays.asList(contributor.toString().split("/"));
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
