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
package org.apache.sling.distribution.serialization.impl.vlt;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.osgi.service.component.annotations.Activate;

/**
 *
 */
@Component(metatype = true,
        label = "Apache Sling Distribution Serialization - FileVault Contetn Serializer Factory",
        description = "OSGi configuration for FileVault content serializer",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionContentSerializer.class)
@Property(name = "webconsole.configurationFactory.nameHint", value = "Format name: {name}")
public class FileVaultContentSerializerFactory implements DistributionContentSerializer {

    /**
     * name of this package builder.
     */
    @Property(label = "Name", description = "The name of the serialization format.")
    private static final String NAME = DistributionComponentConstants.PN_NAME;

    /**
     * import mode property for file vault package builder
     */
    @Property(label = "Import Mode", description = "The vlt import mode for created packages.")
    private static final String IMPORT_MODE = "importMode";

    /**
     * ACL handling property for file vault package builder
     */
    @Property(label = "Acl Handling", description = "The vlt acl handling mode for created packages.")
    private static final String ACL_HANDLING = "aclHandling";

    /**
     * Package roots
     */
    @Property(label = "Package Roots", description = "The package roots to be used for created packages. (this is useful for assembling packages with an user that cannot read above the package root)")
    private static final String PACKAGE_ROOTS = "package.roots";

    /**
     * Package filters
     */
    @Property(label = "Package Filters", description = "The package path filters. Filter format: path|+include|-exclude", cardinality = 100)
    private static final String PACKAGE_FILTERS = "package.filters";


    @Property(label = "Use Binary References", description = "If activated, it avoids sending binaries in the distribution package.", boolValue = false)
    public static final String USE_BINARY_REFERENCES = "useBinaryReferences";

    @Reference
    private Packaging packaging;

    private FileVaultContentSerializer fileVaultContentSerializer;

    @Activate
    protected void activate(Map<String, Object> config) {

        String name = PropertiesUtil.toString(config.get(NAME), null);
        String importModeString = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(IMPORT_MODE), null));
        String aclHandlingString = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(ACL_HANDLING), null));

        String[] packageRoots = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PACKAGE_ROOTS), null));
        String[] packageFilters = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PACKAGE_FILTERS), null));

        boolean useBinaryReferences = PropertiesUtil.toBoolean(config.get(USE_BINARY_REFERENCES), false);

        ImportMode importMode = null;
        if (importModeString != null) {
            importMode = ImportMode.valueOf(importModeString.trim());
        }

        AccessControlHandling aclHandling = null;
        if (aclHandlingString != null) {
            aclHandling = AccessControlHandling.valueOf(aclHandlingString.trim());
        }

        fileVaultContentSerializer = new FileVaultContentSerializer(name, packaging, importMode, aclHandling, packageRoots, packageFilters, useBinaryReferences);
    }


    @Override
    public void extractToStream(ResourceResolver resourceResolver, DistributionRequest request, OutputStream outputStream) throws DistributionException {
        fileVaultContentSerializer.extractToStream(resourceResolver, request, outputStream);
    }

    @Override
    public void importFromStream(ResourceResolver resourceResolver, InputStream stream) throws DistributionException {
        fileVaultContentSerializer.importFromStream(resourceResolver, stream);
    }

    @Override
    public String getName() {
        return fileVaultContentSerializer.getName();
    }
}
