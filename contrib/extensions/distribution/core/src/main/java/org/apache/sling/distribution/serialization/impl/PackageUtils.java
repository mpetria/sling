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


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.annotation.Nonnull;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;


public class PackageUtils {
    private static Object repolock = new Object();
    private static Object filelock = new Object();

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

        Node refs = JcrUtils.getOrAddNode(parent, "refs", NodeType.NT_UNSTRUCTURED);

    }


    public static void acquire(Resource resource, @Nonnull String[] holderNames) throws RepositoryException {
        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        Node parent = resource.adaptTo(Node.class);

        Node refs = parent.getNode("refs");

        for (String holderName : holderNames) {
            if (!refs.hasNode(holderName)) {
                refs.addNode(holderName, NodeType.NT_UNSTRUCTURED);
            }
        }
    }


    public static boolean release(Resource resource, @Nonnull String[] holderNames) throws RepositoryException {
        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        Node parent = resource.adaptTo(Node.class);

        Node refs = parent.getNode("refs");

        for (String holderName : holderNames) {
            Node refNode = refs.getNode(holderName);
            if (refNode != null) {
                refNode.remove();
            }
        }

        if (!refs.hasNodes()) {
            refs.remove();
            return true;
        }

        return false;
    }


    public static void acquire(File file, @Nonnull String[] holderNames) throws IOException {

        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        synchronized (filelock) {
            try {
                HashSet<String> set = new HashSet<String>();

                if (file.exists()) {
                    ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
                    set = (HashSet<String>) inputStream.readObject();
                    IOUtils.closeQuietly(inputStream);
                }

                set.addAll(Arrays.asList(holderNames));

                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(set);
                IOUtils.closeQuietly(outputStream);

            } catch (ClassNotFoundException e) {

            }
        }


    }


    public static boolean release(File file, @Nonnull String[] holderNames) throws IOException {
        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        synchronized (filelock) {
            try {

                HashSet<String> set = new HashSet<String>();

                if (file.exists()) {
                    ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
                    set = (HashSet<String>) inputStream.readObject();
                    IOUtils.closeQuietly(inputStream);
                }

                set.removeAll(Arrays.asList(holderNames));

                if (set.isEmpty()) {
                    FileUtils.deleteQuietly(file);
                    return true;
                }

                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(set);
                IOUtils.closeQuietly(outputStream);
            }
            catch (ClassNotFoundException e) {

            }
        }
        return false;
    }
}
