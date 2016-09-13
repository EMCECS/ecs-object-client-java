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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import com.emc.object.Protocol;
import com.emc.object.util.RestUtilTest;
import com.emc.rest.smart.ecs.Vdc;

/**
 * @author seibed
 *
 */
public class S3ConfigTest extends Assert {

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

        s3Config.setProperty("prop1", "value");
        ArrayList<String> prop2 = new ArrayList<String>(2);
        prop2.add("string");
        prop2.add("strung");
        s3Config.setProperty("prop2", prop2);
        runTests(s3Config);

        s3Config = new S3Config(Protocol.HTTPS, new Vdc("jink", "jank", "junk"), new Vdc("whatever"), new Vdc("dummy"));
        s3Config.setPort(S3Config.DEFAULT_HTTPS_PORT);
        runTests(s3Config);

        s3Config.setUserAgent("agent" + new String(RestUtilTest.OHM_UTF8, "UTF-8"));
        runTests(s3Config);

        s3Config = new S3Config(Protocol.HTTPS, new Vdc("jink", "jank" + RestUtilTest.OHM_UTF8, "junk"), new Vdc("whatever"), new Vdc("dummy"));
        s3Config.setPort(S3Config.DEFAULT_HTTPS_PORT);
        runTests(s3Config);

        s3Config.setProperty("prop1", "value");
        prop2 = new ArrayList<String>(2);
        prop2.add("string" + new String(RestUtilTest.OHM_UTF8, "UTF-8"));
        prop2.add("strung");
        s3Config.setProperty("prop2", prop2);
        runTests(s3Config);
}

    /**
     * @param s3Config
     * @throws Exception 
     */
    private void runTests(S3Config s3Config) throws Exception {
        String configUri = S3Config.toConfigUri(s3Config);
        System.out.println(configUri);
        S3Config s3Config2 = S3Config.fromConfigUri(configUri);
        assertEquals(configUri, S3Config.toConfigUri(s3Config2));
        compare(s3Config, s3Config2);
    }

    /**
     * @param s3Config
     * @param s3Config2
     */
    private void compare(S3Config s3Config, S3Config s3Config2) {
        assertEquals(s3Config.getChunkedEncodingSize(), s3Config2.getChunkedEncodingSize());
        assertEquals(s3Config.getFaultInjectionRate(), s3Config2.getFaultInjectionRate(), 0.0001);
        assertEquals(s3Config.getIdentity(), s3Config2.getIdentity());
        assertEquals(s3Config.getInitialRetryDelay(), s3Config2.getInitialRetryDelay());
        assertEquals(s3Config.getNamespace(), s3Config2.getNamespace());
        assertEquals(s3Config.getPort(), s3Config2.getPort());
        assertEquals(s3Config.getProtocol().toString(), s3Config2.getProtocol().toString());
        assertEquals(s3Config.getRetryBufferSize(), s3Config2.getRetryBufferSize());
        assertEquals(s3Config.getRetryLimit(), s3Config2.getRetryLimit());
        assertEquals(s3Config.getRootContext(), s3Config2.getRootContext());
        assertEquals(s3Config.getSecretKey(), s3Config2.getSecretKey());
        assertEquals(s3Config.getServerClockSkew(), s3Config2.getServerClockSkew());
        assertEquals(s3Config.getUserAgent(), s3Config2.getUserAgent());
        assertEquals(s3Config.isChecksumEnabled(), s3Config2.isChecksumEnabled());
        assertEquals(s3Config.isGeoPinningEnabled(), s3Config2.isGeoPinningEnabled());
        assertEquals(s3Config.isGeoReadRetryFailover(), s3Config2.isGeoReadRetryFailover());
        assertEquals(s3Config.isRetryEnabled(), s3Config2.isRetryEnabled());
        assertEquals(s3Config.isSignNamespace(), s3Config2.isSignNamespace());
        assertEquals(s3Config.isSmartClient(), s3Config2.isSmartClient());
        assertEquals(s3Config.isUseVHost(), s3Config2.isUseVHost());
        for (Entry<String, Object> entry : s3Config.getProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                assertEquals(entry.getValue(), s3Config2.getProperty(entry.getKey()));
            } else if (entry.getValue() instanceof List){
                List list = (List)entry.getValue();
                List list2 = (List)s3Config2.getProperty(entry.getKey());
                assertTrue(list.containsAll(list2) && list2.containsAll(list));
            }
        }
        assertEquals(s3Config.getVdcs(), s3Config2.getVdcs());
    }

}
