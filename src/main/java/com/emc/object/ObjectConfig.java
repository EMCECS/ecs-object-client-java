/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object;

import com.emc.rest.smart.SmartConfig;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ObjectConfig<T extends ObjectConfig<T>> {
    private static final Logger l4j = Logger.getLogger(ObjectConfig.class);

    public static final String PROPERTY_POLL_PROTOCOL = "com.emc.object.pollProtocol";
    public static final String PROPERTY_POLL_PORT = "com.emc.object.pollPort";
    public static final String PROPERTY_POLL_INTERVAL = "com.emc.object.pollInterval";
    public static final String PROPERTY_DISABLE_POLLING = "com.emc.object.disablePolling";
    public static final String PROPERTY_PROXY_URI = "com.emc.object.proxyUri";
    public static final String PROPERTY_PROXY_USER = "com.emc.object.proxyUser";
    public static final String PROPERTY_PROXY_PASS = "com.emc.object.proxyPass";

    public static final String PACKAGE_VERSION = ObjectConfig.class.getPackage().getImplementationVersion();
    public static final String DEFAULT_USER_AGENT = "ECS Java SDK" + (PACKAGE_VERSION != null ? " v" + PACKAGE_VERSION : "");

    private Protocol protocol;
    private List<String> hosts;
    private int port = -1;
    private String rootContext;
    private String namespace;
    private String identity;
    private String secretKey;
    private long serverClockSkew;
    private String userAgent = DEFAULT_USER_AGENT;
    private EncryptionConfig encryptionConfig;

    private Map<String, Object> properties = new HashMap<String, Object>();

    public ObjectConfig(Protocol protocol, int port, String... hostList) {
        this.protocol = protocol;
        this.port = port;
        this.hosts = Arrays.asList(hostList);
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public int getPort() {
        return port;
    }

    public String getRootContext() {
        return rootContext;
    }

    public void setRootContext(String rootContext) {
        if (rootContext != null) {

            // remove first & last slash
            rootContext = rootContext.trim().replaceAll("^/", "").replaceAll("/$", "");
        }
        this.rootContext = rootContext;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public long getServerClockSkew() {
        return serverClockSkew;
    }

    public void setServerClockSkew(long serverClockSkew) {
        this.serverClockSkew = serverClockSkew;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public EncryptionConfig getEncryptionConfig() {
        return encryptionConfig;
    }

    public void setEncryptionConfig(EncryptionConfig encryptionConfig) {
        this.encryptionConfig = encryptionConfig;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Object property(String propName) {
        return properties.get(propName);
    }

    public String propAsString(String propName) {
        Object value = property(propName);
        return value == null ? null : value.toString();
    }

    /**
     * Allows custom properties to be set. These will be passed on to other client components (i.e. Jersey ClientConfig)
     */
    public void property(String propName, Object value) {
        properties.put(propName, value);
    }

    @SuppressWarnings("unchecked")
    public T withRootContext(String rootContext) {
        setRootContext(rootContext);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withNamespace(String namespace) {
        setNamespace(namespace);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withIdentity(String identity) {
        setIdentity(identity);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withSecretKey(String secretKey) {
        setSecretKey(secretKey);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withUserAgent(String userAgent) {
        setUserAgent(userAgent);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public ObjectConfig withEncryptionConfig(EncryptionConfig encryptionConfig) {
        setEncryptionConfig(encryptionConfig);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withProperty(String propName, Object value) {
        property(propName, value);
        return (T) this;
    }

    public abstract String resolveHost();

    /**
     * Resolves a path relative to the API context. The returned URI will be of the format
     * scheme://host[:port]/[rootContext/]relativePath?query. The scheme and port are pulled from the first endpoint in
     * the endpoints list. The host to use may be virtual (to be resolved by a load balancer) or calculated in
     * implementations as round-robin or single-host.
     */
    public URI resolvePath(String relativePath, String query) {
        String path = "/";

        // rootContext should be cleaned by setter
        if (rootContext != null && rootContext.length() > 0) path += rootContext + "/";

        // add relative path to context
        path += relativePath;

        try {
            URI uri = new URI(protocol.toString(), null, resolveHost(), port, path, query, null);

            l4j.debug("raw path & query: " + path + "?" + query);
            l4j.debug("resolved URI: " + uri);

            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI syntax", e);
        }
    }


    public SmartConfig toSmartConfig() {
        SmartConfig smartConfig = new SmartConfig(hosts);

        smartConfig.setDisablePolling(Boolean.parseBoolean(propAsString(properties, PROPERTY_DISABLE_POLLING)));

        if (properties.containsKey(PROPERTY_POLL_INTERVAL)) {
            try {
                smartConfig.setPollInterval(Integer.parseInt(propAsString(properties, PROPERTY_POLL_INTERVAL)));
            } catch (NumberFormatException e) {
                throw new RuntimeException(String.format("invalid poll interval (%s=%s)",
                        PROPERTY_POLL_INTERVAL, properties.get(PROPERTY_POLL_INTERVAL)), e);
            }
        }

        try {
            if (properties.containsKey(PROPERTY_PROXY_URI))
                smartConfig.setProxyUri(new URI(propAsString(PROPERTY_PROXY_URI)));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid proxy URI", e);
        }
        smartConfig.setProxyUser(propAsString(PROPERTY_PROXY_USER));
        smartConfig.setProxyPass(propAsString(PROPERTY_PROXY_PASS));

        for (String prop : properties.keySet()) {
            smartConfig.property(prop, properties.get(prop));
        }

        return smartConfig;
    }

    protected String propAsString(Map<String, Object> properties, String propName) {
        Object value = properties.get(propName);
        return value == null ? null : value.toString();
    }
}
