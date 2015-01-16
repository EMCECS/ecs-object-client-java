package com.emc.object;

import com.emc.rest.smart.SmartConfig;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ObjectConfig {
    private static final Logger l4j = Logger.getLogger(ObjectConfig.class);

    public static final String PROPERTY_POLL_PROTOCOL = "com.emc.object.property.pollProtocol";
    public static final String PROPERTY_POLL_PORT = "com.emc.object.property.pollPort";
    public static final String PROPERTY_POLL_INTERVAL = "com.emc.object.property.pollInterval";
    public static final String PROPERTY_DISABLE_POLLING = "com.emc.object.property.disablePolling";

    private List<URI> endpoints;
    private String rootContext;
    private String namespace;
    private String identity;
    private String secretKey;
    private long serverClockSkew;

    private Map<String, Object> properties = new HashMap<>();

    public List<URI> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<URI> endpoints) {
        this.endpoints = endpoints;
    }

    public String getRootContext() {
        return rootContext;
    }

    public void setRootContext(String rootContext) {
        if (rootContext != null) {

            // replace first & last slash
            rootContext = "/" + rootContext.replaceAll("^/", "").replaceAll("/$", "");

            // if we're left with just "/", set to null
            if (rootContext.equals("/")) rootContext = null;
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

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Object property(String propName) {
        return properties.get(propName);
    }

    public Object propAsString(String propName) {
        Object value = property(propName);
        return value == null ? null : value.toString();
    }

    /**
     * Allows custom properties to be set. These will be passed on to other client components (i.e. Jersey ClientConfig)
     */
    public void property(String propName, Object value) {
        properties.put(propName, value);
    }

    public ObjectConfig withEndpoints(List<URI> endpoints) {
        setEndpoints(endpoints);
        return this;
    }

    public ObjectConfig withRootContext(String rootContext) {
        setRootContext(rootContext);
        return this;
    }

    public ObjectConfig withNamespace(String namespace) {
        setNamespace(namespace);
        return this;
    }

    public ObjectConfig withIdentity(String identity) {
        setIdentity(identity);
        return this;
    }

    public ObjectConfig withSecretKey(String secretKey) {
        setSecretKey(secretKey);
        return this;
    }

    public ObjectConfig withProperty(String propName, Object value) {
        property(propName, value);
        return this;
    }

    public abstract String resolveHost();

    /**
     * Resolves a path relative to the API context. The returned URI will be of the format
     * scheme://host[:port][/rootContext]/relativePath?query. The scheme and port are pulled from the first endpoint in
     * the endpoints list. The host to use may be virtual (to be resolved by a load balancer) or calculated in
     * implementations as round-robin or single-host.
     */
    public URI resolvePath(String relativePath, String query) {
        URI sample = getEndpoints().get(0);
        String path = "";

        // rootContext should be cleaned by setter
        if (rootContext != null) path = rootContext;

        // make sure we delimit rootContext and relative path
        if (!relativePath.startsWith("/")) path += "/";

        // add relative path to context
        path += relativePath;

        try {
            URI uri = new URI(sample.getScheme(), null, resolveHost(), sample.getPort(), path, query, null);

            l4j.debug("raw path & query: " + path + "?" + query);
            l4j.debug("resolved URI: " + uri);

            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI syntax", e);
        }
    }


    public SmartConfig toSmartConfig() {
        List<String> hosts = new ArrayList<>();
        for (URI uri : endpoints) {
            hosts.add(uri.getHost());
        }

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
