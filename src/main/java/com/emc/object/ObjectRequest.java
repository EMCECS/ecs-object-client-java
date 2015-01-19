package com.emc.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ObjectRequest {
    private String namespace;

    /**
     * Implement to return the HTTP method used for the request
     */
    public abstract Method getMethod();

    /**
     * Implement to return the context-relative path of the request (i.e. the object key/path). Be sure to exclude
     * dynamic path properties such as bucket or namespace. Since this is context-relative, also exclude the base
     * context of the service (i.e. /rest for Atmos).
     */
    public abstract String getPath();

    /**
     * Implement to return the request-specific querystring based on properties of the request.
     */
    public abstract String getQuery();

    /**
     * Override to return request-specific headers based on properties of the request. Always call super() first and
     * modify the result.
     */
    public Map<String, List<Object>> getHeaders() {
        return new HashMap<>();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
