package com.emc.object;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.Map;

public abstract class ObjectResponse {
    private Map<String, List<Object>> headers;

    public String headerString(String name) {
        if (headers == null) return null;
        List<Object> values = headers.get(name);
        if (values == null || values.isEmpty()) return null;
        return values.get(0).toString();
    }

    @XmlTransient
    public Map<String, List<Object>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<Object>> headers) {
        this.headers = headers;
    }
}
