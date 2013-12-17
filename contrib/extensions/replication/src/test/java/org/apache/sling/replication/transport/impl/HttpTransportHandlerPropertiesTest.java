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


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class HttpTransportHandlerPropertiesTest {

    private final String[] inputTransportProperties;
    private final String inputSelector;
    private final String[] outputHeaders;
    private final String[] outputSinglePathHeaders;
    private final String outputBody;


    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { new String[]{}, "",
                        new String[]{}, new String[]{}, null},
                { new String[]{}, "add",
                        new String[]{}, new String[]{}, null},
                { new String[]{"header.add=Header: Add" }, "add",
                        new String[]{ "Header: Add" }, new String[]{}, null},
                { new String[]{"header.add=Header: Add", "header=Header: Always" }, "add",
                        new String[]{ "Header: Add", "Header: Always" }, new String[]{}, null},
                { new String[]{"header.add=Header: Add", "header=Header: Always", "header.delete=Header:Del" }, "add",
                        new String[]{"Header: Add", "Header: Always" }, new String[]{}, null},
                { new String[]{"header.add=Header: Add", "header=Header: Always" }, "delete",
                        new String[]{"Header: Always" }, new String[]{}, null},
                { new String[]{"header.add=Header: Add", "header=Header: Always", "body=none" }, "add",
                        new String[] {"Header: Add", "Header: Always" }, new String[]{}, "none"},
                { new String[]{"header.add=Header: Add", "header=Header: Always", "body=none", "header=PathHeader: {path}" }, "add",
                        new String[]{"Header: Add", "Header: Always"}, new String[] {"PathHeader: {path}" }, "none"},
        });

    }

    public HttpTransportHandlerPropertiesTest(String[] inputTransportProperties, String inputSelector, String[] outputHeaders, String[] outputSinglePathHeaders, String outputBody){

        this.inputTransportProperties = inputTransportProperties;
        this.inputSelector = inputSelector;
        this.outputHeaders = outputHeaders;
        this.outputSinglePathHeaders = outputSinglePathHeaders;
        this.outputBody = outputBody;

    }

    @Test
    public void testHttpTransportProperties () {
        HttpTransportHandler.CustomizationData customizationData = HttpTransportHandler.parseTransportProperties(inputTransportProperties, inputSelector);

        String[] headers = customizationData.getHeaders();
        String[] singlePathHeaders = customizationData.getSinglePathHeaders();
        String body = customizationData.getBody();

        assertArrayEquals(outputHeaders, headers);
        assertArrayEquals(outputSinglePathHeaders, singlePathHeaders);
        assertEquals(outputBody, body);
    }
}
