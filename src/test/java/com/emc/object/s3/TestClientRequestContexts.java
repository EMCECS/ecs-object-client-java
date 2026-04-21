/*
 * Copyright (c) 2015, EMC Corporation.
 * Licensed under the BSD 3-Clause License. See LICENSE.txt.
 */
package com.emc.object.s3;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Test utilities for building stub JAX-RS request/response contexts to exercise the {@link S3Signer}
 * and the Jersey 2 filters without needing a live Jersey client.
 */
public final class TestClientRequestContexts {
    private TestClientRequestContexts() {}

    public static StubClientRequestContext request(String method, URI uri) {
        return new StubClientRequestContext(method, uri);
    }

    public static class StubClientRequestContext implements ClientRequestContext {
        private String method;
        private URI uri;
        private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private final Map<String, Object> properties = new HashMap<>();
        private Object entity;

        public StubClientRequestContext(String method, URI uri) {
            this.method = method;
            this.uri = uri;
        }

        public StubClientRequestContext withHeaders(MultivaluedMap<String, Object> h) {
            this.headers = h;
            return this;
        }

        @Override public Object getProperty(String name) { return properties.get(name); }
        @Override public Collection<String> getPropertyNames() { return Collections.unmodifiableSet(properties.keySet()); }
        @Override public void setProperty(String name, Object object) { properties.put(name, object); }
        @Override public void removeProperty(String name) { properties.remove(name); }
        @Override public URI getUri() { return uri; }
        @Override public void setUri(URI uri) { this.uri = uri; }
        @Override public String getMethod() { return method; }
        @Override public void setMethod(String method) { this.method = method; }
        @Override public MultivaluedMap<String, Object> getHeaders() { return headers; }
        @Override public MultivaluedMap<String, String> getStringHeaders() {
            MultivaluedMap<String, String> s = new MultivaluedHashMap<>();
            for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
                for (Object v : e.getValue()) s.add(e.getKey(), v == null ? null : v.toString());
            }
            return s;
        }
        @Override public String getHeaderString(String name) {
            List<Object> vals = headers.get(name);
            if (vals == null || vals.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            for (Object v : vals) { if (sb.length() > 0) sb.append(','); sb.append(v); }
            return sb.toString();
        }
        @Override public Date getDate() { return null; }
        @Override public Locale getLanguage() { return null; }
        @Override public MediaType getMediaType() { return null; }
        @Override public List<MediaType> getAcceptableMediaTypes() { return Collections.emptyList(); }
        @Override public List<Locale> getAcceptableLanguages() { return Collections.emptyList(); }
        @Override public Map<String, Cookie> getCookies() { return Collections.emptyMap(); }
        @Override public boolean hasEntity() { return entity != null; }
        @Override public Object getEntity() { return entity; }
        @Override public Class<?> getEntityClass() { return entity == null ? null : entity.getClass(); }
        @Override public Type getEntityType() { return getEntityClass(); }
        @Override public void setEntity(Object entity) { this.entity = entity; }
        @Override public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) { this.entity = entity; }
        @Override public Annotation[] getEntityAnnotations() { return new Annotation[0]; }
        @Override public OutputStream getEntityStream() { return null; }
        @Override public void setEntityStream(OutputStream outputStream) {}
        @Override public Client getClient() { return null; }
        @Override public Configuration getConfiguration() { return null; }
        @Override public void abortWith(Response response) {}
    }

    public static StubClientResponseContext response(int status) {
        return new StubClientResponseContext(status);
    }

    public static class StubClientResponseContext implements ClientResponseContext {
        private int status;
        private MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        private InputStream entityStream;

        public StubClientResponseContext(int status) {
            this.status = status;
        }

        public StubClientResponseContext withHeader(String name, String value) {
            headers.add(name, value);
            return this;
        }

        public StubClientResponseContext withEntityStream(InputStream in) {
            this.entityStream = in;
            return this;
        }

        @Override public int getStatus() { return status; }
        @Override public void setStatus(int code) { this.status = code; }
        @Override public Response.StatusType getStatusInfo() { return Response.Status.fromStatusCode(status); }
        @Override public void setStatusInfo(Response.StatusType statusInfo) { this.status = statusInfo.getStatusCode(); }
        @Override public MultivaluedMap<String, String> getHeaders() { return headers; }
        @Override public String getHeaderString(String name) {
            List<String> v = headers.get(name);
            return (v == null || v.isEmpty()) ? null : String.join(",", v);
        }
        @Override public Set<String> getAllowedMethods() { return Collections.emptySet(); }
        @Override public Date getDate() { return null; }
        @Override public Locale getLanguage() { return null; }
        @Override public int getLength() { return -1; }
        @Override public MediaType getMediaType() { return null; }
        @Override public Map<String, NewCookie> getCookies() { return Collections.emptyMap(); }
        @Override public javax.ws.rs.core.EntityTag getEntityTag() { return null; }
        @Override public Date getLastModified() { return null; }
        @Override public URI getLocation() { return null; }
        @Override public Set<javax.ws.rs.core.Link> getLinks() { return new HashSet<>(); }
        @Override public boolean hasLink(String relation) { return false; }
        @Override public javax.ws.rs.core.Link getLink(String relation) { return null; }
        @Override public javax.ws.rs.core.Link.Builder getLinkBuilder(String relation) { return null; }
        @Override public boolean hasEntity() { return entityStream != null; }
        @Override public InputStream getEntityStream() { return entityStream; }
        @Override public void setEntityStream(InputStream input) { this.entityStream = input; }
    }
}
