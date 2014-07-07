/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.replication.serialization;

import java.io.IOException;
import java.io.InputStream;

public class SimpleReplicationPackage implements ReplicationPackage {

    private final String action;
    private final String[] paths;
    private final String type;
    private final InputStream stream;

    public SimpleReplicationPackage(String action, String[] paths, String type, InputStream stream) {
        this.action = action;
        this.paths = paths;
        this.type = type;
        this.stream = stream;
    }

    public String getId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getPaths() {
        return paths;
    }

    public String getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

    public InputStream createInputStream() throws IOException {
        return stream;
    }

    public long getLength() {
        return 0;
    }

    public void close() {

    }

    public void delete() {

    }
}
