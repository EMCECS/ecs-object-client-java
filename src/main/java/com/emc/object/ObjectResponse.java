/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.Map;

public abstract class ObjectResponse {
    private Map<String, List<String>> headers;

    public String firstHeader(String name) {
        if (headers == null) return null;
        List<String> values = headers.get(name);
        if (values == null || values.isEmpty()) return null;
        return values.get(0);
    }

    @XmlTransient
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }
}
