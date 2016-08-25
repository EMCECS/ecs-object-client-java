/*
 * Copyright (c) 2015-2016, EMC Corporation.
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

import com.emc.object.util.RestUtil;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.SmartConfig;
import com.emc.rest.smart.ecs.Vdc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ObjectConfig<T extends ObjectConfig<T>> {

    private static final Logger log = LoggerFactory.getLogger(ObjectConfig.class);

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
    public static final int DEFAULT_CHUNKED_ENCODING_SIZE = 2 * 1024 * 1024; // 2MB to match ECS buffer size

    // NOTE: if you add a property, make sure you add it to the cloning constructor!
    private Protocol protocol;
    private List<Vdc> vdcs;
    private int port = -1;
    private boolean smartClient = true;
    private String rootContext;
    private String namespace;
    private String identity;
    private String secretKey;
    private long serverClockSkew;
    private String userAgent = DEFAULT_USER_AGENT;
    private boolean geoPinningEnabled = false;
    private boolean geoReadRetryFailover = false;
    private int chunkedEncodingSize = DEFAULT_CHUNKED_ENCODING_SIZE;

    private Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Single endpoint constructor (disables smart-client).
     */
    public ObjectConfig(URI endpoint) {
        this(Protocol.valueOf(endpoint.getScheme().toUpperCase()), endpoint.getPort(), endpoint.getHost());
        setSmartClient(false);
    }

    /**
     * Single VDC constructor.
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

    /**
     * Cloning constructor.
     */
    public ObjectConfig(ObjectConfig<T> other) {
        this.protocol = other.protocol;
        // deep copy the VDCs to avoid two clients referencing the same host lists (SDK-122)
        this.vdcs = new ArrayList<Vdc>();
        for (Vdc vdc : other.getVdcs()) {
            this.vdcs.add(new Vdc(vdc.getName(), vdc.getHosts()));
        }
        this.port = other.port;
        this.smartClient = other.smartClient;
        this.rootContext = other.rootContext;
        this.namespace = other.namespace;
        this.identity = other.identity;
        this.secretKey = other.secretKey;
        this.serverClockSkew = other.serverClockSkew;
        this.userAgent = other.userAgent;
        this.geoPinningEnabled = other.geoPinningEnabled;
        this.geoReadRetryFailover = other.geoReadRetryFailover;
        this.properties = new HashMap<String, Object>(other.properties);
    }

    public abstract Host resolveHost();

    /**
     * Resolves a path relative to the API context. The returned URI will be of the format
     * scheme://host[:port][/rootContext]/subPath?query. The scheme and port are pulled from the first endpoint in
     * the endpoints list. The host to use may be virtual (to be resolved by a load balancer) or calculated in
     * implementations as round-robin or single-host. Note this is not to be confused with the client-side load
     * balancing provided by the smart client, which overrides any host set here.
     *
     * Note that the query string must already be encoded. This is the only way too allow ampersands (&amp;) as part
     * of a paremeter value
     */
    public URI resolvePath(String subPath, String rawQuery) {
        String path = "";

        // rootContext should be cleaned by setter
        if (rootContext != null && rootContext.length() > 0) path += rootContext;

        // add relative path to context
        path += subPath;
        if (path.isEmpty()) path = "/";

        try {
            URI uri = RestUtil.buildUri(protocol.toString().toLowerCase(), resolveHost().getName(), port, path, rawQuery, null);

            log.debug("raw path & query: " + path + "?" + rawQuery);
            log.debug("resolved URI: " + uri);

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

        if (!smartClient || Boolean.parseBoolean(propAsString(properties, PROPERTY_DISABLE_HEALTH_CHECK)))
            smartConfig.setHealthCheckEnabled(false);
        if (!smartClient || Boolean.parseBoolean(propAsString(properties, PROPERTY_DISABLE_HOST_UPDATE)))
            smartConfig.setHostUpdateEnabled(false);

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

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSmartClient() {
        return smartClient;
    }

    public void setSmartClient(boolean smartClient) {
        this.smartClient = smartClient;
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

    /**
     * @deprecated (2.0.3) always returns null (see {@link #setEncryptionConfig(EncryptionConfig)})
     */
    public EncryptionConfig getEncryptionConfig() {
        return null;
    }

    /**
     * @deprecated (2.0.3) this method does nothing. EncryptionConfig instance should be passed to the constructor of
     * an encryption client
     */
    public void setEncryptionConfig(EncryptionConfig encryptionConfig) {
    }

    public boolean isGeoPinningEnabled() {
        return geoPinningEnabled;
    }

    public void setGeoPinningEnabled(boolean geoPinningEnabled) {
        this.geoPinningEnabled = geoPinningEnabled;
    }

    public boolean isGeoReadRetryFailover() {
        return geoReadRetryFailover;
    }

    public void setGeoReadRetryFailover(boolean geoReadRetryFailover) {
        this.geoReadRetryFailover = geoReadRetryFailover;
    }

    public int getChunkedEncodingSize() {
        return chunkedEncodingSize;
    }

    public void setChunkedEncodingSize(int chunkedEncodingSize) {
        this.chunkedEncodingSize = chunkedEncodingSize;
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
    public T withPort(int port) {
        setPort(port);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withSmartClient(boolean smartClient) {
        setSmartClient(smartClient);
        return (T) this;
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

    /**
     * @deprecated (2.0.3) this method does nothing. EncryptionConfig instance should be passed to the constructor of
     * an encryption client
     */
    @SuppressWarnings("unchecked")
    public T withEncryptionConfig(EncryptionConfig encryptionConfig) {
        setEncryptionConfig(encryptionConfig);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withGeoPinningEnabled(boolean geoPinningEnabled) {
        setGeoPinningEnabled(geoPinningEnabled);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withChunkedEncodingSize(int chunkedEncodingSize) {
        setChunkedEncodingSize(chunkedEncodingSize);
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
                ", smartClient=" + smartClient +
                ", rootContext='" + rootContext + '\'' +
                ", namespace='" + namespace + '\'' +
                ", identity='" + identity + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", serverClockSkew=" + serverClockSkew +
                ", userAgent='" + userAgent + '\'' +
                ", geoPinningEnabled=" + geoPinningEnabled +
                ", geoReadRetryFailover=" + geoReadRetryFailover +
                ", properties=" + properties +
                '}';
    }
}
