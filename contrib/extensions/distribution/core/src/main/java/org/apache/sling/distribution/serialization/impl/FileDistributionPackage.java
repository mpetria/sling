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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;

/**
 * A {@link DistributionPackage} based on a {@link File}.
 */
public class FileDistributionPackage implements DistributionPackage {

    private final File file;
    private final String type;
    private final DistributionPackageInfo info;

    public FileDistributionPackage(@Nonnull File file, @Nonnull String type) {
        this.info = new DistributionPackageInfo(type);
        this.file = file;
        this.type = type;

        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
        //this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, paths);
    }

    @Nonnull
    public String getId() {
        return file.getAbsolutePath();
    }

    @Nonnull
    public String getType() {
        return type;
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        return new PackageInputStream(file);
    }

    @Override
    public long getSize() {
        return file.length();
    }

    public void close() {
        // do nothing
    }

    public void delete() {
        assert file.delete();
    }

    @Nonnull
    @Override
    public DistributionPackageInfo getInfo() {
        return info;
    }

    public File getFile() {
        return file;
    }


    public class PackageInputStream extends BufferedInputStream {
        private final File file;

        public PackageInputStream(File file) throws IOException {
            super(FileUtils.openInputStream(file));

            this.file = file;
        }


        public File getFile() {
            return file;
        }
    }

}