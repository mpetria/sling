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
package org.apache.sling.replication.serialization.impl;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationParameter;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

@Component(label = "Default Replication Package Exporter")
@Service(value = ReplicationPackageExporter.class)
@Property(name = "name", value = DefaultReplicationPackageExporter.NAME)
public class DefaultReplicationPackageExporter implements ReplicationPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());


    @Property(label = "Name")
    public static final String NAME = "reverserepo";

    @Property(label = "Queue")
    private static final String QUEUE_NAME = "queue";

    @Reference(name = "ReplicationAgent", target = "(name=reverserepo)", policy = ReferencePolicy.STATIC)
    private ReplicationAgent agent;

    private String queueName;



    @Activate
    public void activate(BundleContext context, Map<String, ?> config) throws Exception {
        queueName = PropertiesUtil.toString(config.get(QUEUE_NAME), "");
    }


    public ReplicationPackage exportPackage() {

        try {
            // TODO : consider using queue distribution strategy and validating who's making this request
            log.info("getting item from queue {}", queueName);

            // get first item
            ReplicationPackage head = agent.removeHead(queueName);
            return head;
        }
        catch (Exception ex) {
            log.error("Error exporting package", ex);
        }

        return null;
    }
}
