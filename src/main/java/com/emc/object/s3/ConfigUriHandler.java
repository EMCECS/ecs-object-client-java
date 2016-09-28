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
package com.emc.object.s3;

import com.emc.object.Protocol;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.ecs.Vdc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Config URI-to-S3Config converter. URI format is
 * <pre>
 *     http[s]://host[:port][/?&lt;param&gt;=&lt;value&gt;][&amp;&lt;param&gt;=&lt;value&gt;][...]
 * </pre>
 * Note: Each Vdc specification is a
 * comma-separated list of names - the first is the Vdc name, and the rest
 * are host names belonging to that Vdc. If the list has only one name, that
 * is the Vdc name and the only host name
 */
class ConfigUriHandler {
    private static final Logger log = LoggerFactory.getLogger(ConfigUriHandler.class);

    private static final String ACCESS_KEY = "accessKey";
    private static final String CHECKSUM_ENABLED = "checksumEnabled";
    private static final String CHUNKED_ENCODING_SIZE = "chunkedEncodingSize";
    private static final String ENABLE_VHOST = "enableVhost";
    private static final String FAULT_INJECTION_RATE = "faultInjectionRate";
    private static final String GEO_PINNING_ENABLED = "geoPinningEnabled";
    private static final String GEO_READ_RETRY_FAILOVER = "geoReadRetryFailover";
    private static final String NAMESPACE = "namespace";
    private static final String RETRY_BUFFER_SIZE = "retryBufferSize";
    private static final String RETRY_ENABLED = "retryEnabled";
    private static final String RETRY_INITIAL_DELAY = "initialRetryDelay";
    private static final String RETRY_LIMIT = "retryLimit";
    private static final String ROOT_CONTEXT = "rootContext";
    private static final String VDC = "vdc";
    private static final String SECRET_KEY = "secretKey";
    private static final String SERVER_CLOCK_SKEW = "serverClockSkew";
    private static final String SIGN_NAMESPACE = "signNamespace";
    private static final String SMART_CLIENT = "smartClient";
    private static final String USER_AGENT = "userAgent";
    private static final String SIGN_METADATA_SEARCH = "signMetadataSearch";

    private static final String CUSTOM_PROPERTY_PREFIX = "X-";

    private final URI uri;
    private final Map<String, List<String>> queryMap = new HashMap<String, List<String>>();
    private final S3Config s3Config;

    /**
     * @param uriString The Config-URI representation of the S3Config.
     */
    public ConfigUriHandler(String uriString) throws Exception {
        uri = new URI(uriString);
        loadQueryMap(uri.getQuery());
        s3Config = makeConfig();
    }

    /**
     * Load parameters from the URI query.
     */
    private void loadQueryMap(String query) {
        if (isNotBlank(query)) {
            String[] queryParts = query.split("&");
            for (String queryPart : queryParts) {
                int equalsIndex = queryPart.indexOf("=");
                String parameter = (equalsIndex < 0) ? queryPart : queryPart.substring(0, equalsIndex);
                if (isNotBlank(parameter)) {
                    parameter = parameter.trim();
                    String value = null;
                    if (equalsIndex > 0) {
                        value = (equalsIndex == (queryPart.length() - 1)) ? "" : queryPart.substring(equalsIndex + 1);
                    }
                    setProperty(parameter, value);
                }
            }
        }
    }

    public ConfigUriHandler(S3Config config) {
        s3Config = new S3Config(config);
        loadQueryMap();
        uri = makeURI();
    }

    /**
     * Load parameters from the existing s3Config.
     */
    @SuppressWarnings("rawtypes")
    private void loadQueryMap() {
        setProperty(ENABLE_VHOST, s3Config.isUseVHost());
        setProperty(ACCESS_KEY, s3Config.getIdentity());
        setProperty(SECRET_KEY, s3Config.getSecretKey());
        setProperty(SMART_CLIENT, s3Config.isSmartClient());
        for (Vdc vdc : s3Config.getVdcs()) {
            StringBuilder stringBuilder = new StringBuilder();
            String separator = "";
            for (Host host : vdc.getHosts()) {
                stringBuilder.append(separator).append(host.getName());
                separator = ",";
            }
            setProperty(VDC, stringBuilder.toString());
        }
        setProperty(CHECKSUM_ENABLED, s3Config.isChecksumEnabled());
        setProperty(CHUNKED_ENCODING_SIZE, s3Config.getChunkedEncodingSize());
        setProperty(FAULT_INJECTION_RATE, s3Config.getFaultInjectionRate());
        setProperty(GEO_PINNING_ENABLED, s3Config.isGeoPinningEnabled());
        setProperty(GEO_READ_RETRY_FAILOVER, s3Config.isGeoReadRetryFailover());
        setProperty(NAMESPACE, s3Config.getNamespace());
        setProperty(RETRY_BUFFER_SIZE, s3Config.getRetryBufferSize());
        setProperty(RETRY_ENABLED, s3Config.isRetryEnabled());
        setProperty(RETRY_INITIAL_DELAY, s3Config.getInitialRetryDelay());
        setProperty(RETRY_LIMIT, s3Config.getRetryLimit());
        setProperty(ROOT_CONTEXT, s3Config.getRootContext());
        setProperty(SERVER_CLOCK_SKEW, s3Config.getServerClockSkew());
        setProperty(SIGN_NAMESPACE, s3Config.isSignNamespace());
        setProperty(USER_AGENT, s3Config.getUserAgent());
        setProperty(SIGN_METADATA_SEARCH, s3Config.isSignMetadataSearch());
        for (Entry<String, Object> entry : s3Config.getProperties().entrySet()) {
            String key = CUSTOM_PROPERTY_PREFIX + entry.getKey();
            if (entry.getValue() instanceof List) {
                for (Object value : (List) entry.getValue()) {
                    setProperty(key, value.toString());
                }
            } else {
                setProperty(key, entry.getValue().toString());
            }
        }
    }

    private void setProperty(String propertyName, String value) {
        if (value == null) {
            queryMap.put(propertyName, null);
        } else {
            List<String> list = queryMap.get(propertyName);
            if (list == null) {
                list = new ArrayList<String>();
                queryMap.put(propertyName, list);
            }
            list.add(value);
        }
    }

    private void setProperty(String propertyName, boolean value) {
        setProperty(propertyName, Boolean.toString(value));
    }

    private void setProperty(String propertyName, int value) {
        setProperty(propertyName, Integer.toString(value));
    }

    private void setProperty(String propertyName, float value) {
        setProperty(propertyName, Float.toString(value));
    }

    private void setProperty(String propertyName, long value) {
        setProperty(propertyName, Long.toString(value));
    }

    private URI makeURI() {
        String scheme = s3Config.getProtocol().toString();
        String host = s3Config.getVdcs().get(0).getName();
        int port = s3Config.getPort();
        String query = makeQuery();
        try {
            return new URI(scheme, null, host, port, null, query, null);
        } catch (URISyntaxException e) {
            log.error("error constructing config URI", e);
        }
        return null;
    }

    private String makeQuery() {
        if (queryMap.isEmpty()) {
            return null;
        }

        String separator = "";
        StringBuilder stringBuilder = new StringBuilder();
        for (Entry<String, List<String>> entry : queryMap.entrySet()) {
            if (entry.getValue() == null) {
                stringBuilder.append(separator).append(entry.getKey());
                separator = "&";
            } else {
                for (String value : entry.getValue()) {
                    stringBuilder.append(separator).append(entry.getKey()).append("=");
                    separator = "&";
                    if (isNotBlank(value)) {
                        stringBuilder.append(value);
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    S3Config getConfig() {
        return new S3Config(s3Config);
    }

    String getUriString() {
        return uri.toString();
    }

    /**
     * @param string The String to check.
     * @return <ul>
     *         <li><code>true</code> if <code>string</code> is null or
     *         <code>string.trim()</code> is empty.</li>
     *         <li><code>false</code> otherwise.</li>
     *         </ul>
     */
    private static boolean isNotBlank(String string) {
        return (string != null) && (!string.trim().isEmpty());
    }

    private S3Config makeConfig() throws Exception {
        boolean enableVhost = Boolean.parseBoolean(getProperty(ENABLE_VHOST, Boolean.FALSE.toString()));
        String accessKey = getProperty(ACCESS_KEY);
        String secretKey = getProperty(SECRET_KEY);
        boolean smartClient = Boolean.parseBoolean(getProperty(SMART_CLIENT, Boolean.FALSE.toString()));

        S3Config s3Config;
        if (enableVhost) {
            s3Config = new S3Config(uri).withUseVHost(true);
        } else {
            Protocol protocol = Protocol.valueOf(uri.getScheme().toUpperCase());
            String host = uri.getHost();
            int port = uri.getPort();
            List<String> vdcProperties = getProperties(VDC);
            ArrayList<Vdc> vdcs = new ArrayList<Vdc>();
            boolean hostNotIncluded = true;
            if (vdcProperties != null) {
                for (String vdcNameList : vdcProperties) {
                    String[] vdcNames = vdcNameList.split(",");
                    if ((vdcNames.length > 0) && isNotBlank(vdcNames[0])) {
                        if (hostNotIncluded && host.equals(vdcNames[0])) {
                            hostNotIncluded = false;
                        }
                        vdcs.add(new Vdc(vdcNames));
                    }
                }
            }
            if (hostNotIncluded) {
                vdcs.add(new Vdc(host));
            }

            s3Config = new S3Config(protocol, vdcs.toArray(new Vdc[vdcs.size()]));
            if (port > 0) {
                s3Config.setPort(port);
            }
        }

        String value = getProperty(CHECKSUM_ENABLED);
        if (value != null) {
            s3Config.setChecksumEnabled(Boolean.parseBoolean(value));
        }

        value = getProperty(CHUNKED_ENCODING_SIZE);
        if (value != null) {
            s3Config.setChunkedEncodingSize(Integer.parseInt(value));
        }

        value = getProperty(FAULT_INJECTION_RATE);
        if (value != null) {
            s3Config.setFaultInjectionRate(Float.parseFloat(value));
        }

        value = getProperty(GEO_PINNING_ENABLED);
        if (value != null) {
            s3Config.setGeoPinningEnabled(Boolean.parseBoolean(value));
        }

        value = getProperty(GEO_READ_RETRY_FAILOVER);
        if (value != null) {
            s3Config.setGeoReadRetryFailover(Boolean.parseBoolean(value));
        }

        value = getProperty(NAMESPACE);
        if (value != null) {
            s3Config.setNamespace(value);
        }

        value = getProperty(RETRY_BUFFER_SIZE);
        if (value != null) {
            s3Config.setRetryBufferSize(Integer.parseInt(value));
        }

        value = getProperty(RETRY_ENABLED);
        if (value != null) {
            s3Config.setRetryEnabled(Boolean.parseBoolean(value));
        }

        value = getProperty(RETRY_INITIAL_DELAY);
        if (value != null) {
            s3Config.setInitialRetryDelay(Integer.parseInt(value));
        }

        value = getProperty(RETRY_LIMIT);
        if (value != null) {
            s3Config.setRetryLimit(Integer.parseInt(value));
        }

        value = getProperty(ROOT_CONTEXT);
        if (value != null) {
            s3Config.setRootContext(value);
        }

        value = getProperty(SERVER_CLOCK_SKEW);
        if (value != null) {
            s3Config.setServerClockSkew(Long.parseLong(value));
        }

        value = getProperty(SIGN_NAMESPACE);
        if (value != null) {
            s3Config.setSignNamespace(Boolean.parseBoolean(value));
        }

        value = getProperty(USER_AGENT);
        if (value != null) {
            s3Config.setUserAgent(value);
        }

        value = getProperty(SIGN_METADATA_SEARCH);
        if (value != null) {
            s3Config.setSignMetadataSearch(Boolean.parseBoolean(value));
        }

        for (Entry<String, List<String>> entry : queryMap.entrySet()) {
            if (entry.getKey().startsWith(CUSTOM_PROPERTY_PREFIX)) {
                Object customValue = (entry.getValue().size() == 1) ? entry.getValue().get(0) : entry.getValue();
                s3Config.setProperty(entry.getKey().substring(CUSTOM_PROPERTY_PREFIX.length()), customValue);
            }
        }

        return s3Config.withIdentity(accessKey).withSecretKey(secretKey).withSmartClient(smartClient);
    }

    private String getProperty(String key, String defaultValue) throws Exception {
        String value = getProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private String getProperty(String key) throws Exception {
        List<String> values = getProperties(key);
        String value = null;
        if (values != null) {
            if (values.size() > 1) {
                throw new Exception("Too many values specified for " + key);
            }
            value = values.get(0);
        }
        return value;
    }

    private List<String> getProperties(String key) {
        return queryMap.get(key);
    }

}
