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

import com.emc.object.AbstractClientTest;
import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.s3.bean.AbstractVersion;
import com.emc.object.s3.bean.EncodingType;
import com.emc.object.s3.bean.S3Object;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.ListObjectsRequest;
import com.emc.object.s3.request.ListVersionsRequest;
import com.emc.object.util.TestProperties;
import com.emc.rest.smart.LoadBalancer;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.util.TestConfig;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assume;

import java.net.URI;
import java.util.Arrays;
import java.util.Properties;

public abstract class AbstractS3ClientTest extends AbstractClientTest {
    private static final Logger l4j = Logger.getLogger(AbstractS3ClientTest.class);

    protected S3Client client;

    protected abstract S3Client createS3Client() throws Exception;

    protected final void initClient() throws Exception {
        this.client = createS3Client();
    }

    @After
    public void dumpLBStats() {
        if (client != null) {
            LoadBalancer loadBalancer = ((S3JerseyClient) client).getLoadBalancer();
            l4j.info(Arrays.toString(loadBalancer.getHostStats()));
        }
    }

    @After
    public void shutdownClient() {
        if (client != null) client.destroy();
    }

    @Override
    protected void createBucket(String bucketName) throws Exception {
        client.createBucket(bucketName);
    }

    @Override
    protected void cleanUpBucket(String bucketName) throws Exception {
        if (client != null && client.bucketExists(bucketName)) {
            if (client.getBucketVersioning(bucketName).getStatus() != null) {
                for (AbstractVersion version : client.listVersions(new ListVersionsRequest(bucketName).withEncodingType(EncodingType.url)).getVersions()) {
                    client.deleteVersion(bucketName, version.getKey(), version.getVersionId());
                }
            } else {
                for (S3Object object : client.listObjects(new ListObjectsRequest(bucketName).withEncodingType(EncodingType.url)).getObjects()) {
                    client.deleteObject(bucketName, object.getKey());
                }
            }
            client.deleteBucket(bucketName);
        }
    }

    protected S3Config createS3Config() throws Exception {
        return s3ConfigFromProperties();
    }

    protected S3Config s3ConfigFromProperties() throws Exception {
        Properties props = TestConfig.getProperties();

        String accessKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ACCESS_KEY);
        String secretKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_SECRET_KEY);
        URI endpoint = new URI(TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ENDPOINT));
        boolean enableVhost = Boolean.parseBoolean(props.getProperty(TestProperties.ENABLE_VHOST));
        boolean disableSmartClient = Boolean.parseBoolean(props.getProperty(TestProperties.DISABLE_SMART_CLIENT));
        String proxyUri = props.getProperty(TestProperties.PROXY_URI);

        S3Config s3Config;
        if (enableVhost) {
            s3Config = new S3Config(endpoint).withUseVHost(true);
        } else if (endpoint.getPort() > 0) {
            s3Config = new S3Config(Protocol.valueOf(endpoint.getScheme().toUpperCase()), new Vdc(endpoint.getHost()));
            s3Config.setPort(endpoint.getPort());
        } else {
            s3Config = new S3Config(Protocol.valueOf(endpoint.getScheme().toUpperCase()), endpoint.getHost());
        }
        s3Config.withIdentity(accessKey).withSecretKey(secretKey);

        if (proxyUri != null) s3Config.setProperty(ObjectConfig.PROPERTY_PROXY_URI, proxyUri);

        if(disableSmartClient)
            s3Config.setSmartClient(false);
        // uncomment to hit a single node
        //s3Config.property(ObjectConfig.PROPERTY_DISABLE_POLLING, true);

        return s3Config;
    }
}
