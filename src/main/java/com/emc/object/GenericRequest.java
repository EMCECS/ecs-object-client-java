package com.emc.object;

public class GenericRequest extends ObjectRequest {
    private String query;

    public GenericRequest(Method method, String path) {
        super(method, path);
    }

    @Override
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public GenericRequest withQuery(String query) {
        setQuery(query);
        return this;
    }
}
