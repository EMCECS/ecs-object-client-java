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

import com.emc.rest.smart.Host;
import com.emc.rest.smart.SmartConfig;
import com.emc.rest.smart.ecs.Vdc;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public abstract class ObjectConfig<T extends ObjectConfig<T>> {
    private static final Logger l4j = Logger.getLogger(ObjectConfig.class);

    public static final String PROPERTY_POLL_PROTOCOL = "com.emc.object.pollProtocol";
    public static final String PROPERTY_POLL_PORT = "com.emc.object.pollPort";
    public static final String PROPERTY_POLL_INTERVAL = "com.emc.object.pollInterval";
    public static final String PROPERTY_DISABLE_HEALTH_CHECK = "com.emc.object.disableHealthCheck";
    public static final String PROPERTY_DISABLE_HOST_UPDATE = "com.emc.object.disableHostUpdate";
    public static final String PROPERTY_PROXY_URI = "com.emc.object.proxyUri";
    public static final String PROPERTY_PROXY_USER = "com.emc.object.proxyUser";
    public static final String PROPERTY_PROXY_PASS = "com.emc.object.proxyPass";

    public static final String PACKAGE_VERSION = ObjectConfig.class.getPackage().getImplementationVersion();
    public static final String DEFAULT_USER_AGENT = String.format("ECS Java SDK%s Java/%s (%s; %s; %s)",
            (PACKAGE_VERSION != null ? " v" + PACKAGE_VERSION : ""), System.getProperty("java.version"),
            System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));

    private Protocol protocol;
    private List<Vdc> vdcs;
    private int port = -1;
    private String rootContext;
    private String namespace;
    private String identity;
    private String secretKey;
    private long serverClockSkew;
    private String userAgent = DEFAULT_USER_AGENT;
    private EncryptionConfig encryptionConfig;

    private Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Single VDC or virtual host constructor.
     */
    public ObjectConfig(Protocol protocol, int port, String... hosts) {
        this(protocol, port, new Vdc(hosts));
    }

    /**
     * Multiple VDC constructor.
     */
    public ObjectConfig(Protocol protocol, int port, Vdc... vdcs) {
        this.protocol = protocol;
        this.port = port;
        this.vdcs = Arrays.asList(vdcs);
    }

    public abstract Host resolveHost();

    /**
     * Resolves a path relative to the API context. The returned URI will be of the format
     * scheme://host[:port]/[rootContext/]relativePath?query. The scheme and port are pulled from the first endpoint in
     * the endpoints list. The host to use may be virtual (to be resolved by a load balancer) or calculated in
     * implementations as round-robin or single-host. Note this is not to be confused with the client-side load
     * balancing provided by the smart client, which overrides any host set here.
     */
    public URI resolvePath(String relativePath, String query) {
        String path = "/";

        // rootContext should be cleaned by setter
        if (rootContext != null && rootContext.length() > 0) path += rootContext + "/";

        // add relative path to context
        path += relativePath;

        try {
            URI uri = new URI(protocol.toString(), null, resolveHost().getName(), port, path, query, null);

            l4j.debug("raw path & query: " + path + "?" + query);
            l4j.debug("resolved URI: " + uri);

            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI syntax", e);
        }
    }


    public SmartConfig toSmartConfig() {
        List<Host> allHosts = new ArrayList<Host>();
        for (Vdc vdc : vdcs) {
            allHosts.addAll(vdc.getHosts());
        }

        SmartConfig smartConfig = new SmartConfig(allHosts);

        smartConfig.setHealthCheckEnabled(!Boolean.parseBoolean(propAsString(properties, PROPERTY_DISABLE_HEALTH_CHECK)));
        smartConfig.setHostUpdateEnabled(!Boolean.parseBoolean(propAsString(properties, PROPERTY_DISABLE_HOST_UPDATE)));

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
                smartConfig.setProxyUri(new URI(getPropAsString(PROPERTY_PROXY_URI)));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid proxy URI", e);
        }
        smartConfig.setProxyUser(getPropAsString(PROPERTY_PROXY_USER));
        smartConfig.setProxyPass(getPropAsString(PROPERTY_PROXY_PASS));

        for (String prop : properties.keySet()) {
            smartConfig.withProperty(prop, properties.get(prop));
        }

        return smartConfig;
    }

    protected String propAsString(Map<String, Object> properties, String propName) {
        Object value = properties.get(propName);
        return value == null ? null : value.toString();
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public List<Vdc> getVdcs() {
        return vdcs;
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

    public Object getProperty(String propName) {
        return properties.get(propName);
    }

    public String getPropAsString(String propName) {
        Object value = getProperty(propName);
        return value == null ? null : value.toString();
    }

    /**
     * Allows custom properties to be set. These will be passed on to other client components (i.e. Jersey ClientConfig)
     */
    public void setProperty(String propName, Object value) {
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
        setProperty(propName, value);
        return (T) this;
    }

    @Override
    public String toString() {
        return "ObjectConfig{" +
                "protocol=" + protocol +
                ", vdcs=" + vdcs +
                ", port=" + port +
                ", rootContext='" + rootContext + '\'' +
                ", namespace='" + namespace + '\'' +
                ", identity='" + identity + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", serverClockSkew=" + serverClockSkew +
                ", userAgent='" + userAgent + '\'' +
                ", encryptionConfig=" + encryptionConfig +
                ", properties=" + properties +
                '}';
    }
}
