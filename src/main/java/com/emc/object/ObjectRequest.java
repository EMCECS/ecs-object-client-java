/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
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
    private Map<String, Object> properties = new HashMap<>();

    /**
     * @param method      the HTTP method to use for the request
     * @param path        the context-relative path of the request (i.e. the object key/path). Be sure to exclude
     *                    dynamic path properties such as bucket or namespace. Since this is context-relative, also exclude
     *                    the base context of the service (i.e. /rest for Atmos).
     * @param subresource the subresource of the request. This will be the first parameter in the querystring and will
     *                    not have an associated value (i.e. "acl" => ?acl).
     */
    public ObjectRequest(Method method, String path, String subresource) {
        this.method = method;
        this.path = path;
        this.subresource = subresource;
    }

    /**
     * Override to return the request-specific query parameters based on properties of the request. Do NOT include the
     * subresource in this map; it will be inserted automatically.
     *
     * Note this implementation uses a TreeSet, which will sort the parameters by name. This is done to make URLs
     * consistent for testing and should not change the semantics of any request.
     */
    public Map<String, String> getQueryParams() {
        return new TreeMap<>();
    }

    /**
     * Override to return request-specific headers based on properties of the request. Always call super() first and
     * modify the result.
     */
    public Map<String, List<Object>> getHeaders() {
        return new HashMap<>();
    }

    public final String getQueryString() {
        String paramString = RestUtil.generateQueryString(getQueryParams());

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
