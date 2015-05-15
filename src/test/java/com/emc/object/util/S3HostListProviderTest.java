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
package com.emc.object.util;

import com.emc.object.s3.S3HostListProvider;
import com.emc.rest.smart.SmartConfig;
import com.emc.util.TestConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class S3HostListProviderTest {
    @Test
    public void testS3HostListProvider() throws Exception {
        Properties properties = TestConfig.getProperties();

        URI serverURI = new URI(TestConfig.getPropertyNotEmpty(properties, TestProperties.S3_ENDPOINT));
        String user = TestConfig.getPropertyNotEmpty(properties, TestProperties.S3_ACCESS_KEY);
        String secret = TestConfig.getPropertyNotEmpty(properties, TestProperties.S3_SECRET_KEY);
        String proxyUri = properties.getProperty(TestProperties.PROXY_URI);

        ClientConfig clientConfig = new DefaultClientConfig();
        if (proxyUri != null) clientConfig.getProperties().put(ApacheHttpClient4Config.PROPERTY_PROXY_URI, proxyUri);
        Client client = ApacheHttpClient4.create(clientConfig);

        SmartConfig smartConfig = new SmartConfig(Collections.singletonList(serverURI.getHost()));

        S3HostListProvider hostListProvider = new S3HostListProvider(client, smartConfig.getLoadBalancer(), user, secret);
        hostListProvider.setProtocol(serverURI.getScheme());
        hostListProvider.setPort(serverURI.getPort());

        List<String> hostList = hostListProvider.getHostList();

        Assert.assertTrue("server list is empty", hostList.size() > 0);
    }
}
