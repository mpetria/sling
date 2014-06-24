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
