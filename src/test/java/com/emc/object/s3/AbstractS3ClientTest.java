/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3;

import com.emc.object.AbstractClientTest;
import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.s3.bean.S3Object;
import com.emc.object.util.TestProperties;
import com.emc.util.TestConfig;

import java.net.URI;
import java.util.Properties;

public abstract class AbstractS3ClientTest extends AbstractClientTest {
    protected S3Client client;

    @Override
    protected void createBucket(String bucketName) throws Exception {
        client.createBucket(bucketName);
    }

    @Override
    protected void cleanUpBucket(String bucketName) throws Exception {
        try {
            for (S3Object object : client.listObjects(bucketName).getObjects()) {
                client.deleteObject(bucketName, object.getKey());
            }
            client.deleteBucket(bucketName);
        } catch (S3Exception e) {
            if (!"NoSuchBucket".equals(e.getErrorCode())) throw e;
        }
    }

    protected S3Config createS3Config() throws Exception {
        Properties props = TestConfig.getProperties();

        String accessKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ACCESS_KEY);
        String secretKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_SECRET_KEY);
        URI endpoint = new URI(TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ENDPOINT));
        boolean enableVhost = Boolean.parseBoolean(props.getProperty(TestProperties.ENABLE_VHOST));
        String proxyUri = props.getProperty(TestProperties.PROXY_URI);

        S3Config s3Config;
        if (enableVhost) {
            s3Config = new S3VHostConfig(endpoint);
        } else {
            s3Config = new S3Config(Protocol.valueOf(endpoint.getScheme().toUpperCase()), endpoint.getHost());
        }
        s3Config.withIdentity(accessKey).withSecretKey(secretKey);

        if (proxyUri != null) s3Config.property(ObjectConfig.PROPERTY_PROXY_URI, proxyUri);

        // uncomment to hit a single node
        //s3Config.property(ObjectConfig.PROPERTY_DISABLE_POLLING, true);

        return s3Config;
    }
}
