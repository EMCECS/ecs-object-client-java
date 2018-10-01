/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object;

import com.emc.object.util.RestUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ObjectRequest {
    private String namespace;
    private Method method;
    private String path;
    private String subresource;
    private Map<String, List<Object>> customHeaders = new HashMap<String, List<Object>>();
    private Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * @param method      the HTTP method to use for the request
     * @param path        the context-relative path of the request (i.e. the object key/path). Be sure to exclude
     *                    dynamic path properties such as bucket or namespace. Since this is context-relative, also exclude
     *                    the base context of the service (i.e. /rest for Atmos).
     * @param subresource the subresource of the request. This will be the first parameter in the querystring and will
     *                    not have an associated value (i.e. "acl" =&gt; ?acl).
     */
    public ObjectRequest(Method method, String path, String subresource) {
        this.method = method;
        this.path = path;
        this.subresource = subresource;
    }

    /**
     * Constructs a duplicate of the specified object request.
     */
    public ObjectRequest(ObjectRequest other) {
        this.namespace = other.namespace;
        this.method = other.method;
        this.path = other.path;
        this.subresource = other.subresource;
        this.properties.putAll(other.properties);
    }

    /**
     * Override to return the request-specific query parameters based on properties of the request. Do NOT include the
     * subresource in this map; it will be inserted automatically.
     *
     * Note this implementation uses a TreeSet, which will sort the parameters by name. This is done to make URLs
     * consistent for testing and should not change the semantics of any request.
     */
    public Map<String, String> getQueryParams() {
        return new TreeMap<String, String>();
    }

    /**
     * Override to return request-specific headers based on properties of the request. Always call super() first and
     * modify the result.
     */
    public Map<String, List<Object>> getHeaders() {
        return new HashMap<String, List<Object>>(customHeaders);
    }

    public void addCustomHeader(String key, Object value) {
        RestUtil.putSingle(customHeaders, key, value);
    }

    public Map<String, List<Object>> getCustomHeaders() {
        return customHeaders;
    }

    /**
     * @deprecated (2.0.4) use {@link #getRawQueryString()} instead
     */
    public final String getQueryString() {
        return getRawQueryString();
    }

    public final String getRawQueryString() {
        String paramString = RestUtil.generateRawQueryString(getQueryParams());

        String queryString = "";

        if (subresource != null) queryString += subresource;

        if (paramString.length() > 0) queryString += "&" + paramString;

        if (queryString.startsWith("&")) queryString = queryString.substring(1);

        return (queryString.length() > 0) ? queryString : null;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    protected void setPath(String path) {
        this.path = path;
    }

    public String getSubresource() {
        return subresource;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Pass request-specific properties to the HTTP client implementation that may affect
     * processing/filters, etc.
     */
    public void property(String name, Object value) {
        properties.put(name, value);
    }
}
