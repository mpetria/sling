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
