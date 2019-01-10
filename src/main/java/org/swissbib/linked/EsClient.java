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

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Sebastian Schüpbach
 * @version 0.1
 *          <p>
 *          Created on 08.06.16
 */
class EsClient {

    private final static Logger LOG = LoggerFactory.getLogger(EsClient.class);

    TransportClient client;

    EsClient(LocalSettings settings) {
        this.connect(settings);
    }

    private void connect(LocalSettings localSettings) {
        Settings settings = Settings.builder()
                .put("cluster.name", localSettings.getEsCluster())
                .build();
        LOG.info("Connecting to Elasticsearch cluster {} on {}:{}",
                localSettings.getEsCluster(),
                localSettings.getEsHost(),
                localSettings.getEsPort());
        client = new PreBuiltTransportClient(settings);
        try {
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(localSettings.getEsHost()), localSettings.getEsPort()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    void disconnect() {
        LOG.info("Closing Elasticsearch transport client.");
        client.close();
    }


}
