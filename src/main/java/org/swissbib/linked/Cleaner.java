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
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * @author Sebastian Schüpbach
 * @version 0.1
 *          <p>
 *          Created on 07.06.16
 */
abstract class Cleaner implements Connector {

    private final static Logger LOG = LoggerFactory.getLogger(Cleaner.class);

    LocalSettings localSettings;

    TransportClient esClient;

    Cleaner(LocalSettings s) {
        this.localSettings = s;
    }

    public Cleaner connect() {
        Settings settings = Settings.builder()
                .put("cluster.name", localSettings.getEsCluster())
                .build();
        LOG.info("Connecting to Elasticsearch cluster {} on {}:{}",
                localSettings.getEsCluster(),
                localSettings.getEsHost(),
                localSettings.getEsPort());
        esClient = new PreBuiltTransportClient(settings);
        try {
            esClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(localSettings.getEsHost()), localSettings.getEsPort()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Cleaner execute() {
        List<String> sr = identify();
        clean(sr);
        return this;
    }

    public void disconnect() {
        LOG.info("Closing Elasticsearch transport client.");
        esClient.close();
    }

    abstract List<String> identify();

    abstract void clean(List<String> ids);

    final BulkProcessor setBulkProcessor() {
        return BulkProcessor.builder(this.esClient, new BulkProcessor.Listener() {

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
                .setBulkActions(localSettings.getBulkSize())
                .setConcurrentRequests(1)
                .build();
    }

}
