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

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionPackage;

/**
 * Default implementation of a {@link org.apache.sling.distribution.serialization.DistributionPackageBuilder}.
 * It supports writing to either files or resources.
 */
public class DefaultDistributionPackageBuilder extends AbstractDistributionPackageBuilder {

    private static final String PREFIX_PATH = "/var/sling/distribution/";

    private final String packagesPath;
    private final DistributionContentSerializer distributionContentSerializer;
    private final DistributionPackagePersistenceType persistence;

    public DefaultDistributionPackageBuilder(DistributionPackagePersistenceType persistence,
                                             DistributionContentSerializer distributionContentSerializer) {
        super(distributionContentSerializer.getName());
        this.persistence = persistence;
        this.distributionContentSerializer = distributionContentSerializer;
        this.packagesPath = PREFIX_PATH + distributionContentSerializer.getName() + "/data";
    }

    @Override
    protected DistributionPackage createPackageForAdd(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException {
        DistributionPackage distributionPackage;
        if (DistributionPackagePersistenceType.RESOURCE.equals(persistence)) {
            // TODO : write to file if size > threshold
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            distributionContentSerializer.extractToStream(resourceResolver, request, outputStream);
            byte[] buf = outputStream.toByteArray();
            InputStream inputStream = new ByteArrayInputStream(buf);
            try {
                distributionPackage = ResourceDistributionPackage.fromStream(inputStream, buf.length, getType(),
                        resourceResolver, packagesPath);
            } catch (PersistenceException e) {
                throw new DistributionException(e);
            }
        } else {
            try {
                String type = distributionContentSerializer.getName();
                File file = File.createTempFile(packagesPath, "." + type);
                OutputStream outputStream = new FileOutputStream(file);
                distributionContentSerializer.extractToStream(resourceResolver, request, outputStream);
                distributionPackage = new FileDistributionPackage(file, type);
            } catch (Exception e) {
                throw new DistributionException(e);
            }
        }
        return distributionPackage;
    }

    @Override
    protected DistributionPackage readPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream)
            throws DistributionException {
        if (DistributionPackagePersistenceType.RESOURCE.equals(persistence)) {
            try {
                return ResourceDistributionPackage.fromStream(stream, -1, getType(), resourceResolver, packagesPath);
            } catch (PersistenceException e) {
                throw new DistributionException(e);
            }
        } else {
            try {
                File file = File.createTempFile(packagesPath, "." + distributionContentSerializer.getName());
                IOUtils.copy(stream, new FileOutputStream(file));
                return new FileDistributionPackage(file, packagesPath);
            } catch (Exception e) {
                throw new DistributionException(e);
            }
        }
    }

    @Override
    protected boolean installPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage
            distributionPackage) throws DistributionException {
        try {
            distributionContentSerializer.importFromStream(resourceResolver, distributionPackage.createInputStream());
            return true;
        } catch (IOException e) {
            throw new DistributionException(e);
        }
    }

    @Override
    protected DistributionPackage getPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull String
            id) {
        if (DistributionPackagePersistenceType.RESOURCE.equals(persistence)) {
            return new ResourceDistributionPackage(resourceResolver.getResource(id), getType(), resourceResolver);
        } else {
            return new FileDistributionPackage(new File(id), packagesPath);
        }
    }
}
