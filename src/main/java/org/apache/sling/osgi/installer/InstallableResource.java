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
package org.apache.sling.osgi.installer;

import java.io.InputStream;
import java.util.Dictionary;

/** A piece of data that can be installed by the OSGi controller.
 * 	Wraps either a Dictionary or an InputStream.
 *  Extension is used to decide which type of data (bundle, config, etc.). 
 */
public class InstallableResource {
	private final String url;
	private final String extension;
	private final String digest;
	private final InputStream inputStream;
	private final Dictionary<String, Object> dictionary;
	private int priority;
	
	/** Default resource priority */
	public static final int DEFAULT_PRIORITY = 100;
	
	/** Create an empty data object, used when removing resources */
	public InstallableResource(String url) {
		this.url = url;
		this.extension = getExtension(url);
		this.inputStream = null;
		this.dictionary = null;
		this.digest = null;
		this.priority = DEFAULT_PRIORITY;
	}
	
	/** Create a data object that wraps an InputStream 
	 *  @param url unique URL of the supplied data, must start with the scheme used 
	 *     {@link OsgiInstaller#registerResources} call
	 *  @param is the resource contents
	 *  @param digest must be supplied by client. Does not need to be an actual digest
	 *     of the contents, but must change if the contents change. Having this supplied
	 *     by the client avoids having to compute real digests to find out if a resource
	 *     has changed, which can be expensive.        
	 */
	public InstallableResource(String url, InputStream is, String digest) {
		this.url = url;
		this.extension = getExtension(url);
		this.inputStream = is;
		this.dictionary = null;
		this.digest = digest;
        this.priority = DEFAULT_PRIORITY;
	}
	
	/** Create a data object that wraps a Dictionary. Digest will be computed
	 *  by the installer in this case, as configuration dictionaries are 
	 *  usually small so computing a real digest to find out if they changed
	 *  is ok.
	 *  
     *  @param url unique URL of the supplied data, must start with the scheme used 
     *     {@link OsgiInstaller#registerResources} call
     *  @param is the resource contents
	 */
	public InstallableResource(String url, Dictionary<String, Object> d) {
		this.url = url;
		this.extension = getExtension(url);
		this.inputStream = null;
		this.dictionary = d;
        this.digest = null;
        this.priority = DEFAULT_PRIORITY;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ", url=" + url;
	}
	
	/** Compute the extension */ 
	private static String getExtension(String url) {
		final int pos = url.lastIndexOf('.');
		return (pos < 0 ? "" : url.substring(pos+1));
	}
	
	/** Return this data's URL. It is opaque for the {@link OsgiInstaller}
	 * 	but the scheme must be the one used in the 
	 * 	{@link OsgiInstaller#registerResources} call.
	 */
	public String getUrl() {
		return url;
	}

	/** Return this resource's extension, based on its URL */
	public String getExtension() {
		return extension;
	}

	/** Return an input stream with the data of this resource. Null if resource
	 *  contains a dictionary instead. Caller is responsible for closing the stream.
	 */
	public InputStream getInputStream() {
		return inputStream;
	}

	/** Return this resource's dictionary. Null if resource contains an InputStream instead */
	public Dictionary<String, Object> getDictionary() {
		return dictionary;
	}

	/** Return this resource's digest. Not necessarily an actual md5 or other digest of the
	 *  data, can be any string that changes if the data changes. 
	 */
	public String getDigest() {
	    return digest;
	}

	/** Return the priority of this resource. Priorities are used to decide which 
	 *  resource to install when several are registered for the same OSGi entity
	 *  (bundle, config, etc.)
	 */
    public int getPriority() {
        return priority;
    }

    /** Set the priority of this resource */
    public void setPriority(int priority) {
        this.priority = priority;
    }
}
