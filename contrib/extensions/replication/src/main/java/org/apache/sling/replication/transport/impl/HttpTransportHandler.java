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
package org.apache.sling.replication.transport.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basic HTTP POST {@link TransportHandler}
 */
@Component(metatype = false)
@Service(value = TransportHandler.class)
@Property(name = "name", value = HttpTransportHandler.NAME)
public class HttpTransportHandler implements TransportHandler {

    public static final String NAME = "http";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("unchecked")
    public void transport(ReplicationPackage replicationPackage,
                          ReplicationEndpoint replicationEndpoint,
                          TransportAuthenticationProvider<?, ?> transportAuthenticationProvider,
                          String[] transportProperties)
            throws ReplicationTransportException {
        if (log.isInfoEnabled()) {
            log.info("delivering package {} to {} using auth {}",
                    new Object[]{replicationPackage.getId(),
                            replicationEndpoint.getUri(), transportAuthenticationProvider});
        }
        try {
            Executor executor = Executor.newInstance();
            TransportAuthenticationContext context = new TransportAuthenticationContext();
            context.addAttribute("endpoint", replicationEndpoint);
            executor = ((TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProvider)
                    .authenticate(executor, context);

            String[] paths = replicationPackage.getPaths();
            String type = replicationPackage.getType();
            String pathsString = Arrays.toString(paths);
            Request req = Request.Post(replicationEndpoint.getUri()).useExpectContinue()
                    .addHeader(ReplicationHeader.TYPE.toString(), type);

            CustomizationData customizationData = parseTransportProperties(transportProperties);

            if (!customizationData.shouldIgnoreBody() && replicationPackage.getInputStream() != null) {
                req = req.bodyStream(replicationPackage.getInputStream(),
                        ContentType.APPLICATION_OCTET_STREAM);
            }
            Map<String, String> variables = new HashMap<String, String>();
            variables.put("action", replicationPackage.getAction());
            variables.put("path", replicationPackage.getPaths()[0]); // treat all paths
            customizeHeaders(req, variables, customizationData.getHeaderTemplates(), customizationData.getVariableOverrides());

            Response response = executor.execute(req);
            if (response != null) {
                Content content = response.returnContent();
                if (log.isInfoEnabled()) {
                    log.info("Replication content of type {} for {} delivered: {}", new Object[]{
                            type, pathsString, content});
                }
            }
            else {
                throw new IOException("response is empty");
            }
        } catch (Exception e) {
            throw new ReplicationTransportException(e);
        }
    }

    private CustomizationData parseTransportProperties(String[] transportProperties){
        Map<String,String> headerTemplates = new HashMap<String,String>();
        Map<String, String> variableOverrides = new HashMap<String,String>();
        boolean ignoreBody = false;

        for (String property : transportProperties){
            if (property.startsWith("body=")){
                int idx = property.indexOf("=");
                String value = property.substring(idx+1).trim();
                if(value.equals("none")){
                    ignoreBody = true;
                }
            }
            else  if (property.startsWith("header=")){
                int idx = property.indexOf("=");
                String value = property.substring(idx+1).trim();

                idx = value.indexOf(":");
                if(idx >= 0){
                    headerTemplates.put(value.substring(0,idx).trim(), value.substring(idx+1).trim());
                }

            }
            else  if (property.startsWith("override=")){
                int idx = property.indexOf("=");
                String value = property.substring(idx+1).trim();

                idx = value.indexOf(":");
                if(idx >= 0){
                    variableOverrides.put(value.substring(0,idx).trim(), value.substring(idx+1).trim());
                }
            }
        }

        return new CustomizationData(headerTemplates, variableOverrides, ignoreBody);

    }

    private void customizeHeaders(Request reg,  Map<String, String> variables,
                                  Map<String,String> headerTemplates, Map<String, String> variableOverrides) {

        for(String headerName : headerTemplates.keySet()){
            String headerValue = headerTemplates.get(headerName);

            for (String variableName : variables.keySet()){
                String variableValue = variables.get(variableName);
                String variableOverrideName = variableName + "." + variableValue;

                if(variableOverrides.containsKey(variableOverrideName)){
                    variableValue = variableOverrides.get(variableOverrideName);
                }
                headerValue = headerValue.replaceFirst("\\{" + variableName + "\\}", variableValue);
            }

            reg.addHeader(headerName.trim(), headerValue.trim());
        }

    }

    private class CustomizationData {
        Map<String,String> headerTemplates;
        Map<String, String> variableOverrides;
        boolean ignoreBody;

        private CustomizationData(Map<String, String> headerTemplates, Map<String, String> variableOverrides, boolean ignoreBody) {
            this.headerTemplates = headerTemplates;
            this.variableOverrides = variableOverrides;
            this.ignoreBody = ignoreBody;
        }

        public Map<String, String> getHeaderTemplates() {
            return headerTemplates;
        }

        public Map<String, String> getVariableOverrides() {
            return variableOverrides;
        }

        private boolean shouldIgnoreBody() {
            return ignoreBody;
        }
    }


    public boolean supportsAuthenticationProvider(TransportAuthenticationProvider<?, ?> transportAuthenticationProvider) {
        return transportAuthenticationProvider.canAuthenticate(Executor.class);
    }
}