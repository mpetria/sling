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

package org.apache.sling.distribution.serialization.impl;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;

import javax.annotation.Nonnull;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class PackageUtils {
    private static Object repolock = new Object();


    public static Resource getPackagesRoot(ResourceResolver resourceResolver, String packagesRootPath) throws PersistenceException {
        Resource packagesRoot = resourceResolver.getResource(packagesRootPath);

        if (packagesRoot != null) {
            return packagesRoot;
        }

        synchronized (repolock) {
            resourceResolver.refresh();
            packagesRoot = ResourceUtil.getOrCreateResource(resourceResolver, packagesRootPath, "sling:Folder", "sling:Folder", true);
        }

        return packagesRoot;
    }

    public static InputStream getStream(Resource resource) throws RepositoryException {
        Node parent = resource.adaptTo(Node.class);
        InputStream in = parent.getProperty("bin/jcr:content/jcr:data").getBinary().getStream();
        return in;
    }

    public static void uploadStream(Resource resource, InputStream stream) throws RepositoryException {
        Node parent = resource.adaptTo(Node.class);
        Node file = JcrUtils.getOrAddNode(parent, "bin", NodeType.NT_FILE);
        Node content = JcrUtils.getOrAddNode(file, Node.JCR_CONTENT, NodeType.NT_RESOURCE);
        Binary binary = parent.getSession().getValueFactory().createBinary(stream);
        content.setProperty(Property.JCR_DATA, binary);
    }
}
