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
package org.apache.sling.replication.serialization.impl.exporter;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageExporter;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link org.apache.sling.replication.serialization.ReplicationPackageExporter}
 */
@Component(label = "Remote Replication Package Exporter")
@Service(value = ReplicationPackageExporter.class)
@Property(name = "name", value = RemoteReplicationPackageExporter.NAME)
public class RemoteReplicationPackageExporter implements ReplicationPackageExporter {

    public static final String NAME = "remote";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(name = "TransportHandler",
            target = "(name=http-publish-poll)",
            policy = ReferencePolicy.DYNAMIC)
    private TransportHandler transportHandler;

    @Reference(name = "ReplicationPackageBuilder", target = "(name=vlt)", policy = ReferencePolicy.STATIC)
    private ReplicationPackageBuilder packageBuilder;


    public ReplicationPackage exportPackage(ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {

        final List<ReplicationQueueItem> savedReplicationQueueItems = new ArrayList<ReplicationQueueItem>();

        transportHandler.enableProcessing(NAME, new ReplicationQueueProcessor() {
            public boolean process(String queueName, ReplicationQueueItem replicationQueueItem) {
                savedReplicationQueueItems.add(replicationQueueItem);
                return true;
            }
        });

        ReplicationPackage replicationPackage = packageBuilder.createPackage(replicationRequest);
        try {
            transportHandler.transport(NAME, replicationPackage);
        } catch (ReplicationTransportException e) {

        }

        ReplicationPackage responsePackage = null;
        if (savedReplicationQueueItems.size() > 0) {
            ReplicationQueueItem firstReplicationQueueItem = savedReplicationQueueItems.get(0);
            responsePackage = packageBuilder.getPackage(firstReplicationQueueItem.getId());

        }

        transportHandler.disableProcessing(NAME);

        return responsePackage;
    }

    public ReplicationPackage exportPackageById(String replicationPackageId) {
        return packageBuilder.getPackage(replicationPackageId);
    }
}
