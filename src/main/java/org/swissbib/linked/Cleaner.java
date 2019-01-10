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

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * @author Sebastian Schüpbach
 * @version 0.1
 * <p>
 * Created on 07.06.16
 */
class Cleaner {

    private final static Logger LOG = LoggerFactory.getLogger(Cleaner.class);

    private EsClient esClient;
    private BulkProcessor bulkProcessor;

    Cleaner(EsClient esClient) {
        this.esClient = esClient;
        this.bulkProcessor = setBulkProcessor();
    }


    void execute(String index, String docType, ArrayList<String> removedDocuments) {
        for (String id : removedDocuments) {
            bulkProcessor.add(new DeleteRequest(index, docType, id));
        }
    }


    private BulkProcessor setBulkProcessor() {
        return BulkProcessor.builder(this.esClient.client, new BulkProcessor.Listener() {

            @Override
            public void beforeBulk(long l, BulkRequest bulkRequest) {
                LOG.trace("Bulk requests to be processed: {}", bulkRequest.numberOfActions());
            }

            @Override
            public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {
                LOG.trace("Indexing took {} ms", bulkResponse.getTookInMillis());
            }

            @Override
            public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {
                LOG.error("Some errors were reported: {}", throwable.getMessage());
            }
        })
                .setBulkActions(10000)
                .setConcurrentRequests(1)
                .build();
    }

}
