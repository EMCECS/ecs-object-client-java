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
package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.util.RandomInputStream;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;

public class S3JerseyUrlConnectionTest extends S3JerseyClientTest {
    @Override
    protected String getTestBucketPrefix() {
        return "s3-url-connection-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        System.setProperty("http.maxConnections", "100");
        S3Config config = createS3Config();
        String proxy = config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        if (proxy != null) {
            URI proxyUri = new URI(proxy);
            System.setProperty("http.proxyHost", proxyUri.getHost());
            System.setProperty("http.proxyPort", "" + proxyUri.getPort());
        }
        return new S3JerseyClient(config, new URLConnectionClientHandler());
    }

    @Ignore // only run this test against a co-located ECS!
    @Test
    public void testVeryLargeWrite() throws Exception {
        String key = "very-large-object";
        long size = (long) Integer.MAX_VALUE + 102400;
        InputStream content = new RandomInputStream(size);
        S3ObjectMetadata metadata = new S3ObjectMetadata().withContentLength(size);
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(metadata);
        client.putObject(request);

        Assert.assertEquals(size, client.getObjectMetadata(getTestBucket(), key).getContentLength().longValue());
    }

    @Ignore // only run this test against a co-located ECS!
    @Test
    public void testVeryLargeChunkedWrite() throws Exception {
        String key = "very-large-chunked-object";
        long size = (long) Integer.MAX_VALUE + 102400;
        InputStream content = new RandomInputStream(size);
        client.putObject(getTestBucket(), key, content, null);

        Assert.assertEquals(size, client.getObjectMetadata(getTestBucket(), key).getContentLength().longValue());
    }
}
