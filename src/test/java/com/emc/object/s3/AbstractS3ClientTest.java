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
import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.*;
import com.emc.object.util.TestProperties;
import com.emc.rest.smart.LoadBalancer;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.util.TestConfig;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;

public abstract class AbstractS3ClientTest extends AbstractClientTest {
    private static final Logger log = LoggerFactory.getLogger(AbstractS3ClientTest.class);

    protected S3Client client;
    /**
     * may be null
     */
    protected String ecsVersion;
    protected boolean isIamUser = false;
    protected CanonicalUser bucketOwner;

    protected abstract S3Client createS3Client() throws Exception;

    protected final void initClient() throws Exception {
        this.client = createS3Client();
        try {
            this.ecsVersion = client.listDataNodes().getVersionInfo();
        } catch (Exception e) {
            log.warn("could not get ECS version: " + e);
        }
    }

    @Before
    public void checkIamUser() throws IOException {
        Properties props = TestConfig.getProperties();
        this.isIamUser = Boolean.parseBoolean(props.getProperty(TestProperties.S3_IAM_USER));
    }

    @After
    public void dumpLBStats() {
        if (client != null) {
            LoadBalancer loadBalancer = ((S3JerseyClient) client).getLoadBalancer();
            log.info(Arrays.toString(loadBalancer.getHostStats()));
        }
    }

    @Override
    public void shutdownClient() {
        log.debug("shutting down client");
        if (client != null) client.destroy();
    }

    @Override
    protected void createBucket(String bucketName) throws Exception {
        client.createBucket(bucketName);
        this.bucketOwner = client.getBucketAcl(bucketName).getOwner();
    }

    @Override
    protected void cleanUpBucket(String bucketName) {
        if (client != null && client.bucketExists(bucketName)) {
            boolean objectLockEnabled = isIamUser && client.getObjectLockConfiguration(bucketName) != null;
            if (client.getBucketVersioning(bucketName).getStatus() != null) {
                for (AbstractVersion version : client.listVersions(new ListVersionsRequest(bucketName).withEncodingType(EncodingType.url)).getVersions()) {
                    DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName, version.getKey()).withVersionId(version.getVersionId());
                    if (objectLockEnabled) {
                        client.setObjectLegalHold(new SetObjectLegalHoldRequest(bucketName, version.getKey()).withVersionId(version.getVersionId())
                                .withLegalHold(new ObjectLockLegalHold().withStatus(ObjectLockLegalHold.Status.OFF)));
                        deleteRequest.withBypassGovernanceRetention(true);
                    }
                    client.deleteObject(deleteRequest);
                }
            } else {
                for (S3Object object : client.listObjects(new ListObjectsRequest(bucketName).withEncodingType(EncodingType.url)).getObjects()) {
                    client.deleteObject(bucketName, object.getKey());
                }
            }
            client.deleteBucket(bucketName);
        }
    }

    /**
     * call in subclasses if you create MPUs
     */
    protected void cleanMpus(String bucketName) {
        client.listMultipartUploads(new ListMultipartUploadsRequest(bucketName)).getUploads().stream()
                .parallel()
                .forEach(upload -> client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, upload.getKey(), upload.getUploadId())));
    }

    /**
     * this should be the only hook method for generating an S3Config instance - if a test class needs any
     * customizations to the config, override this method and call super, then make your customizations
     */
    protected S3Config createS3Config() throws Exception {
        return s3ConfigFromProperties();
    }

    static S3Config s3ConfigFromProperties() throws Exception {
        Properties props = TestConfig.getProperties();

        URI endpoint = new URI(TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ENDPOINT));
        boolean enableVhost = Boolean.parseBoolean(props.getProperty(TestProperties.ENABLE_VHOST));
        boolean disableSmartClient = Boolean.parseBoolean(props.getProperty(TestProperties.DISABLE_SMART_CLIENT));
        String proxyUriStr = props.getProperty(TestProperties.PROXY_URI);
        String accessKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ACCESS_KEY);
        String secretKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_SECRET_KEY);

        S3Config s3Config;
        if (enableVhost) {
            s3Config = new S3Config(endpoint).withUseVHost(true);
        } else if (endpoint.getPort() > 0) {
            s3Config = new S3Config(Protocol.valueOf(endpoint.getScheme().toUpperCase()), new Vdc(endpoint.getHost()));
            s3Config.setPort(endpoint.getPort());
        } else {
            s3Config = new S3Config(Protocol.valueOf(endpoint.getScheme().toUpperCase()), endpoint.getHost());
        }

        if (proxyUriStr != null) {
            s3Config.setProperty(ObjectConfig.PROPERTY_PROXY_URI, proxyUriStr);
            // in case anything uses URLConnection directly
            URI proxyUri = URI.create(proxyUriStr);
            System.setProperty("http.proxyHost", proxyUri.getHost());
            System.setProperty("http.proxyPort", "" + proxyUri.getPort());
        }

        if (disableSmartClient)
            s3Config.setSmartClient(false);

        s3Config.withIdentity(accessKey).withSecretKey(secretKey);

        return s3Config;
    }
}