/*
 * Copyright (C) 2016 swissbib
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.swissbib.linked;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * @author Sebastian Schüpbach
 * @version 0.1
 *          <p>
 *          Created on 08.06.16
 */
class OrganisationCleaner extends Cleaner {

    private final static Logger LOG = LoggerFactory.getLogger(OrganisationCleaner.class);

    OrganisationCleaner(LocalSettings s) {
        super(s);
    }

    @Override
    final List<String> identify() {
        LOG.info("Start searching for orphaned documents in type organisation and index {}.", localSettings.getEsIndex());
        List<String> idsToRemove = new ArrayList<>();
        try {
            SearchResponse sr = esClient
                    .prepareSearch(localSettings.getEsIndex())
                    .setTypes("organisation")
                    .setQuery(matchAllQuery())
                    .setSize(localSettings.getScrollSize())
                    .setScroll(TimeValue.timeValueMinutes(localSettings.getScrollMinutes()))
                    .execute()
                    .actionGet();
            while (true) {
                for (SearchHit sh : sr.getHits()) {
                    LOG.trace("Processing document with id {} abd type organisation in index {}",
                            sh.id(), localSettings.getEsIndex());
                    long hits = esClient.prepareSearch(localSettings.getEsIndex())
                            .setTypes("bibliographicResource")
                            .setQuery(termQuery("dct:contributor", "http://data.swissbib.ch/organisation/" + sh.id()))
                            .execute()
                            .actionGet()
                            .getHits()
                            .getHits()
                            .length;
                    if (hits == 0) {
                        idsToRemove.add(sh.id());
                        LOG.debug("Mark document with id {} and type organisation in index {} for removal.",
                                sh.id(), localSettings.getEsIndex());
                    }
                }
                sr = esClient.prepareSearchScroll(sr.getScrollId()).setScroll(TimeValue.timeValueMinutes(2)).execute().actionGet();
                if (sr.getHits().getHits().length == 0) break;
            }
        } catch (IndexNotFoundException e) {
            LOG.error("Index {} does not exist in Elasticsearch cluster {} on {}:{}",
                    localSettings.getEsIndex(),
                    localSettings.getEsCluster(),
                    localSettings.getEsHost(),
                    localSettings.getEsPort());
        }

        LOG.info("Search for orphaned documents in type work in index {} finished.", localSettings.getEsIndex());

        return idsToRemove;
    }


    @Override
    final void clean(List<String> ids) {
        BulkProcessor bp = setBulkProcessor();
        LOG.info("Start removal of marked documents in type organisation and index {}.", localSettings.getEsIndex());
        for (String id : ids) {
            LOG.trace("Adding delete request for document {} in type organisation and index {} to the bulk processor",
                    id, localSettings.getEsIndex());
            bp.add(esClient.prepareDelete(localSettings.getEsIndex(), "organisation", id).request());
        }
        LOG.info("Removal of marked documents in type organisation and index {} finished.", localSettings.getEsIndex());
    }
}
