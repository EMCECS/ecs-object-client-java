/*
 * Copyright (c) 2016, EMC Corporation.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.emc.object.Protocol;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.ecs.Vdc;

/**
 * @author seibed
 *
 */
class ConfigURIHandler {

    /**
     * Parameter name for the ECS S3 access key.
     */
    private static final String ACCESS_KEY = "accessKey";

    /**
     * Parameter name for the boolean to decide whether to verify MD5 sums. By
     * default, MD5 sums are verified on whole-object reads and writes whenever
     * possible. You can disable that by setting this parameter to false.
     */
    private static final String CHECKSUM_ENABLED = "checksumEnabled";

    /**
     * Parameter name for the int chunked encoding size.
     * <p>
     * If the parameter is not specified then chunked encoding will not be used.
     * A value &le; 0 declares that chunked encoding will be used with the
     * default chunk size. A value &gt; 0 declares that chunked encoding will be
     * used with the value as the declared chunk size.
     */
    private static final String CHUNKED_ENCODING_SIZE = "chunkedEncodingSize";

    /**
     * Parameter name for the boolean to decide whether to use
     * virtual-host-style requests. Set this parameter to true to enable
     * virtual-host-style requests. This prepends namespaces and buckets as
     * appropriate for each request.
     * <p>
     * <em>NOTE:</em> If you use virtual host configuration, the smart client
     * oprtion will be disabled. You must also specify your namespace or set it
     * to null and include it in the hostname of the endpoint.
     */
    private static final String ENABLE_VHOST = "enableVhost";

    /**
     * Parameter name for the float fault injection rate. Enables fault
     * injection when this parameter is &gt; 0. The rate is a ratio expressed as
     * a decimal between 0 and 1. This is the rate at which faults (HTTP 500
     * errors) should randomly be injected into the response. When faults are
     * injected, the real request is never sent over the wire. Fault injection
     * is disabled by default.
     */
    private static final String FAULT_INJECTION_RATE = "faultInjectionRate";

    /**
     * Parameter name for the boolean to decide whether to enable GeoPinning.
     * Set this parameter to true to enable or false to disable it..
     */
    private static final String GEO_PINNING_ENABLED = "geoPinningEnabled";

    /**
     * Parameter name for the boolean to decide whether to enable
     * GeoReadRetryFailover. Set this parameter to true to enable or false to
     * disable it..
     */
    private static final String GEO_READ_RETRY_FAILOVER = "geoReadRetryFailover";

    /**
     * Parameter name for the S3 namespace to use.
     */
    private static final String NAMESPACE = "namespace";

    /**
     * Parameter name for the int to use as the maximum retry buffer size in
     * bytes.
     */
    private static final String RETRY_BUFFER_SIZE = "retryBufferSize";

    /**
     * Parameter name for the boolean to decide whether to retry failed
     * requests. Set this parameter to false to disable automatic retry of
     * (retriable) requests
     */
    private static final String RETRY_ENABLED = "retryEnabled";

    /**
     * Parameter name for the int number of milliseconds to delay before the
     * first retry attempt after a failed request. The delay time increases by a
     * factor of 2 after each failed request
     */
    private static final String RETRY_INITIAL_DELAY = "initialRetryDelay";

    /**
     * Parameter name for the int number of times to retry failed requests.
     */
    private static final String RETRY_LIMIT = "retryLimit";

    /**
     * Parameter name for the root path to be used in requests.
     */
    private static final String ROOT_CONTEXT = "rootContext";

    /**
     * Parameter name for Vdc specifications. Each Vdc specification is a
     * comma-separated list of names - the first is the Vdc name, and the rest
     * are host names belonging to that Vdc. If the list has only one name, that
     * is the Vdc name and the only host name.
     */
    private static final String VDC = "vdc";

    /**
     * Parameter name for the ECS S3 secret key.
     */
    private static final String SECRET_KEY = "secretKey";

    /**
     * Parameter name for the long number of milliseconds to be added to the
     * date-time object when signing strings.
     */
    private static final String SERVER_CLOCK_SKEW = "serverClockSkew";

    /**
     * Parameter name for the boolean to decide whether to sign the namespace.
     * Standard ECS configurations require signing the namespace to support
     * cross-namespace requests. To change this behavior to a legacy type
     * virtual host, which is isolated to the default namespace of the user, set
     * this to false.
     */
    private static final String SIGN_NAMESPACE = "signNamespace";

    /**
     * Parameter name for the boolean to decide whether to use the smart client.
     * This client should not be used if the S3 instance is behind a firewall.
     */
    private static final String SMART_CLIENT = "smartClient";

    /**
     * Parameter name for the user agent to be used in requests.
     */
    private static final String USER_AGENT = "userAgent";

    /**
     * Parameter prefix for custom properties in requests.
     */
    private static final String CUSTOM_PROPERTY_PREFIX = "X-";

    /**
     * The URI for this ConfigUtil instance.
     */
    private final URI uri;

    /**
     * The property map for this ConfigUtil instance.
     */
    private final Map<String, List<String>> queryMap = new HashMap<String, List<String>>();

    private final S3Config s3Config;

    /**
     * @param uriString A String representation of this ConfigUtil's URI object.
     * @throws Exception
     */
    ConfigURIHandler(String uriString) throws Exception {
        uri = new URI(uriString);
        loadQueryMap(uri.getQuery());
        s3Config = makeConfig();
    }

    /**
     * Load it from the URI query.
     * @param query
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

    /**
     * @param s3Config
     */
    ConfigURIHandler(S3Config config) {
        s3Config = new S3Config(config);
        loadQueryMap();
        uri = makeURI();
    }

    /**
     * Load it from the existing s3Config.
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

    /**
     * @param propertyName
     * @param value
     */
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

    /**
     * @param propertyName
     * @param value
     */
    private void setProperty(String propertyName, boolean value) {
        setProperty(propertyName, Boolean.toString(value));
    }

    /**
     * @param propertyName
     * @param value
     */
    private void setProperty(String propertyName, int value) {
        setProperty(propertyName, Integer.toString(value));
    }

    /**
     * @param propertyName
     * @param value
     */
    private void setProperty(String propertyName, float value) {
        setProperty(propertyName, Float.toString(value));
    }

    /**
     * @param propertyName
     * @param value
     */
    private void setProperty(String propertyName, long value) {
        setProperty(propertyName, Long.toString(value));
    }

    /**
     * @return
     */
    private URI makeURI() {
        String scheme = s3Config.getProtocol().toString();
        String userInfo = null;
        String host = s3Config.getVdcs().get(0).getName();
        int port = s3Config.getPort();
        String path = null;
        String query = makeQuery();
        String fragment = null;
        try {
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return
     */
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
     * @param string
     *            The String to check.
     * @return
     *         <ul>
     *         <li><code>true</code> if <code>string</code> is null or
     *         <code>string.trim()</code> is empty.</li>
     *         <li><code>false</code> otherwise.</li>
     *         </ul>
     */
    private static boolean isNotBlank(String string) {
        return (string != null) && (!string.trim().isEmpty());
    }

    /**
     * @return
     * @throws Exception
     */
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

        for (Entry<String, List<String>> entry : queryMap.entrySet()) {
            if (entry.getKey().startsWith(CUSTOM_PROPERTY_PREFIX)) {
                Object customValue = (entry.getValue().size() == 1) ? entry.getValue().get(0) : entry.getValue();
                s3Config.setProperty(entry.getKey().substring(CUSTOM_PROPERTY_PREFIX.length()), customValue);
            }
        }

        return s3Config.withIdentity(accessKey).withSecretKey(secretKey).withSmartClient(smartClient);
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     * @throws Exception
     */
    private String getProperty(String key, String defaultValue) throws Exception {
        String value = getProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * @param key
     * @return
     * @throws Exception
     */
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

    /**
     * @param key
     * @return
     */
    private List<String> getProperties(String key) {
        return queryMap.get(key);
    }

}
