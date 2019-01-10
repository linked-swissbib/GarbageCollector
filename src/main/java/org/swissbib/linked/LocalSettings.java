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

/**
 * @author Sebastian Schüpbach
 * @version 0.1
 *          <p>
 *          Created on 08.06.16
 */
class LocalSettings {
    private String esHost = "localhost";
    private int esPort = 9300;
    private String esCluster = "elasticsearch";
    private String esIndex = "testsb";

    String getEsHost() {
        return esHost;
    }

    LocalSettings setEsHost(String esHost) {
        this.esHost = esHost;
        return this;
    }

    int getEsPort() {
        return esPort;
    }

    LocalSettings setEsPort(int esPort) {
        this.esPort = esPort;
        return this;
    }

    String getEsCluster() {
        return esCluster;
    }

    LocalSettings setEsCluster(String esCluster) {
        this.esCluster = esCluster;
        return this;
    }

    String getEsIndex() {
        return esIndex;
    }

    LocalSettings setEsIndex(String esIndex) {
        this.esIndex = esIndex;
        return this;
    }

}
