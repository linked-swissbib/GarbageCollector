package org.swissbib.linked;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

class IdFetcher {

    private final static Logger LOG = LoggerFactory.getLogger(IdFetcher.class);

    private EsClient esClient;

    IdFetcher(EsClient esClient) {
        this.esClient = esClient;
    }

    ArrayList<String> execute(String index, String docType, HashSet<String> lookupTable) {
        SearchResponse scrollResp = esClient.client
                .prepareSearch(index)
                .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                .setTypes(docType)
                .setFetchSource(null, "*")
                .setScroll(new TimeValue(60000))
                .setSize(1000)
                .get();
        ArrayList<String> obsoleteDocuments = new ArrayList<>();
        do {
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                if (!lookupTable.contains(hit.getId())) {
                    obsoleteDocuments.add(hit.getId());
                }
                scrollResp = esClient.client
                        .prepareSearchScroll(scrollResp.getScrollId())
                        .setScroll(new TimeValue(60000))
                        .execute()
                        .actionGet();
            }
        } while (scrollResp.getHits().getHits().length != 0);
        return obsoleteDocuments;
    }
}
