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

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.util.ConfigUri;
import com.emc.object.util.RestUtilTest;
import com.emc.rest.smart.SmartConfig;
import com.emc.rest.smart.ecs.Vdc;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map.Entry;

public class ConfigUriS3Test {
    private ConfigUri<S3Config> s3Uri = new ConfigUri<S3Config>(S3Config.class);

    public static final String PROPERTY_POLL_INTERVAL = "com.emc.object.pollInterval";
    public static final String PROPERTY_DISABLE_HEALTH_CHECK = "com.emc.object.disableHealthCheck";
    public static final String PROPERTY_DISABLE_HOST_UPDATE = "com.emc.object.disableHostUpdate";
    public static final String PROPERTY_PROXY_URI = "com.emc.object.proxyUri";
    public static final String PROPERTY_PROXY_USER = "com.emc.object.proxyUser";
    public static final String PROPERTY_PROXY_PASS = "com.emc.object.proxyPass";

    @Test
    public void testToUriConfig() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://dummy"));
        s3Config.setPort(S3Config.DEFAULT_HTTP_PORT);
        runTests(s3Config);

        s3Config.setChecksumEnabled(false);
        runTests(s3Config);

        s3Config.setChunkedEncodingSize(10);
        runTests(s3Config);

        s3Config.setFaultInjectionRate(0.5f);
        runTests(s3Config);

        s3Config.setGeoPinningEnabled(true);
        runTests(s3Config);

        s3Config.setGeoReadRetryFailover(true);
        runTests(s3Config);

        s3Config.setIdentity("junk");
        runTests(s3Config);

        s3Config.setInitialRetryDelay(5);
        runTests(s3Config);

        s3Config.setNamespace("whatever");
        runTests(s3Config);

        s3Config.setPort(5);
        runTests(s3Config);

        s3Config.setRetryBufferSize(10);
        runTests(s3Config);

        s3Config.setRetryEnabled(false);
        runTests(s3Config);

        s3Config.setRetryLimit(5);
        runTests(s3Config);

        s3Config.setRootContext("dummyContext");
        runTests(s3Config);

        s3Config.setSecretKey("secret");
        runTests(s3Config);

        s3Config.setServerClockSkew(-3);
        runTests(s3Config);

        s3Config.setSignNamespace(false);
        runTests(s3Config);

        s3Config.setSmartClient(true);
        runTests(s3Config);

        s3Config.setUserAgent("agent");
        runTests(s3Config);

        s3Config.setUseVHost(false);
        runTests(s3Config);

        s3Config.setSignMetadataSearch(!s3Config.isSignMetadataSearch());
        runTests(s3Config);

        s3Config.setProperty("prop1", "value");
        s3Config.setProperty("prop2", "strung");
        runTests(s3Config);

        s3Config.setReadTimeout(10000);
        s3Config.setConnectTimeout(10000);
        runTests(s3Config);

        s3Config = new S3Config(Protocol.HTTPS, new Vdc("jink", "jank", "junk"), new Vdc("whatever"), new Vdc("dummy"));
        s3Config.setPort(S3Config.DEFAULT_HTTPS_PORT);
        runTests(s3Config);

        s3Config.setUserAgent("agent" + new String(RestUtilTest.OHM_UTF8, "UTF-8"));
        runTests(s3Config);

        s3Config = new S3Config(Protocol.HTTPS, new Vdc("jink", "jank" + new String(RestUtilTest.OHM_UTF8, "UTF-8"), "junk"), new Vdc("whatever"), new Vdc("dummy"));
        s3Config.setPort(S3Config.DEFAULT_HTTPS_PORT);
        runTests(s3Config);

        s3Config.setProperty("prop1", "value");
        s3Config.setProperty("prop2", "string" + new String(RestUtilTest.OHM_UTF8, "UTF-8"));
        runTests(s3Config);
    }

    @Test
    public void testToSmartConfig() throws Exception {
        String dummyString = "dummy";
        Vdc vdc = new Vdc(dummyString);
        int dummyInt = 10;
        S3Config s3Config = new S3Config(Protocol.HTTPS, vdc);
        s3Config.setSmartClient(true);
        s3Config.setProperty(PROPERTY_DISABLE_HEALTH_CHECK, true);
        s3Config.setProperty(PROPERTY_DISABLE_HOST_UPDATE, true);
        s3Config.setProperty(PROPERTY_POLL_INTERVAL, dummyInt);
        s3Config.setProperty(PROPERTY_PROXY_URI, dummyString);
        s3Config.setProperty(PROPERTY_PROXY_USER, dummyString);
        s3Config.setProperty(PROPERTY_PROXY_PASS, dummyString);
        s3Config.setConnectTimeout(dummyInt);
        s3Config.setReadTimeout(dummyInt);
        SmartConfig smartConfig = s3Config.toSmartConfig();
        Assertions.assertTrue(!smartConfig.isHealthCheckEnabled());
        Assertions.assertTrue(!smartConfig.isHostUpdateEnabled());
        Assertions.assertEquals(smartConfig.getProperty(PROPERTY_POLL_INTERVAL), dummyInt);
        Assertions.assertEquals(smartConfig.getProperty(PROPERTY_PROXY_URI), dummyString);
        Assertions.assertEquals(smartConfig.getProperty(PROPERTY_PROXY_USER), dummyString);
        Assertions.assertEquals(smartConfig.getProperty(PROPERTY_PROXY_PASS), dummyString);
        Assertions.assertEquals(smartConfig.getProperty(ClientProperties.CONNECT_TIMEOUT), dummyInt);
        Assertions.assertEquals(smartConfig.getProperty(ClientProperties.READ_TIMEOUT), dummyInt);
    }

    @Test
    public void testDisablePings() {
        // test setting the property directly
        String dummyString = "dummy";
        Vdc vdc = new Vdc(dummyString);
        S3Config s3Config = new S3Config(Protocol.HTTPS, vdc);
        s3Config.setSmartClient(true);
        s3Config.setProperty(ObjectConfig.PROPERTY_DISABLE_HEALTH_CHECK, "true");

        SmartConfig smartConfig = s3Config.toSmartConfig();
        Assertions.assertTrue(smartConfig.isHostUpdateEnabled());
        Assertions.assertFalse(smartConfig.isHealthCheckEnabled());

        // test setting via URI
        s3Config = s3Uri.parseUri("https://dummy?smartClient=true&properties.com.emc.object.disableHealthCheck=true");
        smartConfig = s3Config.toSmartConfig();
        Assertions.assertTrue(smartConfig.isHostUpdateEnabled());
        Assertions.assertFalse(smartConfig.isHealthCheckEnabled());
    }

    private void runTests(S3Config s3Config) throws Exception {
        String configUri = s3Uri.generateUri(s3Config);
        S3Config s3Config2 = s3Uri.parseUri(configUri);
        Assertions.assertEquals(configUri, s3Uri.generateUri(s3Config2));
        compare(s3Config, s3Config2);
    }

    private void compare(S3Config s3Config, S3Config s3Config2) {
        Assertions.assertEquals(s3Config.getChunkedEncodingSize(), s3Config2.getChunkedEncodingSize());
        Assertions.assertEquals(s3Config.getFaultInjectionRate(), s3Config2.getFaultInjectionRate(), 0.0001);
        Assertions.assertEquals(s3Config.getIdentity(), s3Config2.getIdentity());
        Assertions.assertEquals(s3Config.getInitialRetryDelay(), s3Config2.getInitialRetryDelay());
        Assertions.assertEquals(s3Config.getNamespace(), s3Config2.getNamespace());
        Assertions.assertEquals(s3Config.getPort(), s3Config2.getPort());
        Assertions.assertEquals(s3Config.getProtocol().toString(), s3Config2.getProtocol().toString());
        Assertions.assertEquals(s3Config.getRetryBufferSize(), s3Config2.getRetryBufferSize());
        Assertions.assertEquals(s3Config.getRetryLimit(), s3Config2.getRetryLimit());
        if (s3Config.getRootContext() == null) s3Config.setRootContext(""); // null or empty string is ok
        if (s3Config2.getRootContext() == null) s3Config2.setRootContext("");
        Assertions.assertEquals(s3Config.getRootContext(), s3Config2.getRootContext());
        Assertions.assertEquals(s3Config.getSecretKey(), s3Config2.getSecretKey());
        Assertions.assertEquals(s3Config.getServerClockSkew(), s3Config2.getServerClockSkew());
        Assertions.assertEquals(s3Config.getUserAgent(), s3Config2.getUserAgent());
        Assertions.assertEquals(s3Config.isChecksumEnabled(), s3Config2.isChecksumEnabled());
        Assertions.assertEquals(s3Config.isGeoPinningEnabled(), s3Config2.isGeoPinningEnabled());
        Assertions.assertEquals(s3Config.isGeoReadRetryFailover(), s3Config2.isGeoReadRetryFailover());
        Assertions.assertEquals(s3Config.isRetryEnabled(), s3Config2.isRetryEnabled());
        Assertions.assertEquals(s3Config.isSignNamespace(), s3Config2.isSignNamespace());
        Assertions.assertEquals(s3Config.isSmartClient(), s3Config2.isSmartClient());
        Assertions.assertEquals(s3Config.isUseVHost(), s3Config2.isUseVHost());
        Assertions.assertEquals(s3Config.isSignMetadataSearch(), s3Config2.isSignMetadataSearch());
        Assertions.assertEquals(s3Config.getReadTimeout(), s3Config2.getReadTimeout());
        Assertions.assertEquals(s3Config.getConnectTimeout(), s3Config2.getConnectTimeout());
        for (Entry<String, Object> entry : s3Config.getProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                Assertions.assertEquals(entry.getValue(), s3Config2.getProperty(entry.getKey()));
            } else if (entry.getValue() instanceof List<?>) {
                List<?> list = (List<?>) entry.getValue();
                List<?> list2 = (List<?>) s3Config2.getProperty(entry.getKey());
                Assertions.assertTrue(list.containsAll(list2) && list2.containsAll(list));
            }
        }
        Assertions.assertEquals(s3Config.getVdcs(), s3Config2.getVdcs());
    }

}
