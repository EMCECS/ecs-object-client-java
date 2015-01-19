package com.emc.object;

public class GenericRequest extends ObjectRequest {
    private Method method;
    private String path;
    private String query;

    public GenericRequest(Method method, String path, String query) {
        this.method = method;
        this.path = path;
        this.query = query;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getQuery() {
        return query;
    }
}
