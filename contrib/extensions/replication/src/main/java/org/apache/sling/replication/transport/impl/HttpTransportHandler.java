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
import java.util.*;

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

    private static final String PATH_VARIABLE_NAME = "{path}";
    private static final String EMPTY_BODY_PLACEHOLDER = "empty";

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



            CustomizationData customizationData = parseTransportProperties(transportProperties, replicationPackage.getAction());
            String[] paths = replicationPackage.getPaths();

            if(customizationData.isUsingSinglePaths()) {
                // deliver to all paths individually
                for (String path : paths){
                    CustomizationData boundCustomizationData = bindCustomizationDataToPath(customizationData, path);
                    deliverPackage(executor, replicationPackage, replicationEndpoint,
                            boundCustomizationData.getHeaders(), boundCustomizationData.getBody(), path);
                }
            }
            else {
                String pathsString = Arrays.toString(paths);
                deliverPackage(executor, replicationPackage, replicationEndpoint,
                        customizationData.getHeaders(), customizationData.getBody(), pathsString);
            }


        } catch (Exception e) {
            throw new ReplicationTransportException(e);
        }
    }

    private void deliverPackage(Executor executor, ReplicationPackage replicationPackage, ReplicationEndpoint replicationEndpoint,
                                String[] customHeaders, String customBody, String pathsString) throws IOException{
        String type = replicationPackage.getType();


        Request req = Request.Post(replicationEndpoint.getUri()).useExpectContinue()
                .addHeader(ReplicationHeader.TYPE.toString(), type);

        if (customBody == null && replicationPackage.getInputStream() != null) {
            req = req.bodyStream(replicationPackage.getInputStream(),
                    ContentType.APPLICATION_OCTET_STREAM);
        }
        else if(customBody !=null) {
            req = req.bodyString(customBody, ContentType.DEFAULT_TEXT);

        }

        for(String header : customHeaders){
            addHeader(req, header);
        }

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
    }

    private void addHeader(Request req, String header){
        int idx = header.indexOf(":");
        if(idx < 0) return;
        String headerName = header.substring(0, idx).trim();
        String headerValue = header.substring(idx+1).trim();
        req.addHeader(headerName, headerValue);
    }

    public static CustomizationData bindCustomizationDataToPath(CustomizationData customizationData, String path){
        List<String> headers = new ArrayList<String>();
        headers.addAll(Arrays.asList(customizationData.getHeaders()));


        for (String singlePathHeader : customizationData.getSinglePathHeaders()){
            headers.add(singlePathHeader.replace(PATH_VARIABLE_NAME, path));
        }

        return new CustomizationData(headers.toArray(new String[0]), new String[0], customizationData.getBody());
    }


    public static CustomizationData parseTransportProperties(String[] transportProperties, String selector){
        List<String> headers = new ArrayList<String>();
        List<String> singlePathHeaders = new ArrayList<String>();
        String body = null;

        transportProperties = transportProperties == null ? new String[0] : transportProperties;

        for(String property : transportProperties){
            int idx = property.indexOf("=");

            if(idx < 0) continue;

            String propertyKey = property.substring(0, idx).trim();
            String propertyValue = property.substring(idx+1).trim();

            if(propertyKey.length() == 0) continue;

            int idxSelector = propertyKey.indexOf(".");

            String propertySelector = "*";
            if(idxSelector >= 0) {
                propertySelector = propertyKey.substring(idxSelector+1).trim();
                propertyKey = propertyKey.substring(0, idxSelector).trim();

            }

            if(!propertySelector.equals("*") && !propertySelector.equalsIgnoreCase(selector)) continue;

            boolean usingSinglePath = false;
            if(propertyValue.contains(PATH_VARIABLE_NAME)){
                usingSinglePath = true;
            }

            if("body".equalsIgnoreCase(propertyKey)){
                if(EMPTY_BODY_PLACEHOLDER.equals(propertyValue)){
                    propertyValue = "";
                }
                body = propertyValue;
            }
            else if("header".equalsIgnoreCase(propertyKey)){

                if(usingSinglePath){
                    singlePathHeaders.add(propertyValue);
                }
                else{
                    headers.add(propertyValue);
                }

            }
        }
        return new CustomizationData(headers.toArray(new String[0]), singlePathHeaders.toArray(new String[0]), body);
    }

    public static class CustomizationData {
        private String[] headers;
        private String[] singlePathHeaders;
        private String body;

        public CustomizationData(String[] headers, String[] singlePathHeaders, String body) {
            this.headers = headers;
            this.singlePathHeaders = singlePathHeaders;
            this.body = body;
        }


        public String[] getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }

        public boolean isUsingSinglePaths() {
            return singlePathHeaders.length > 0;
        }

        public String[] getSinglePathHeaders() {
            return singlePathHeaders;
        }
    }


    public boolean supportsAuthenticationProvider(TransportAuthenticationProvider<?, ?> transportAuthenticationProvider) {
        return transportAuthenticationProvider.canAuthenticate(Executor.class);
    }
}