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

import com.emc.codec.CodecChain;
import com.emc.object.EncryptionConfig;
import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class S3EncryptionUrlConnectionV4Test extends S3EncryptionClientBasicTest {
    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-url-connection-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        System.setProperty("http.maxConnections", "100");
        S3Config config = createS3Config().withUseV2Signer(false);
        String proxy = config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        if (proxy != null) {
            URI proxyUri = new URI(proxy);
            System.setProperty("http.proxyHost", proxyUri.getHost());
            System.setProperty("http.proxyPort", "" + proxyUri.getPort());
        }
        rclient = new S3JerseyClient(config, new URLConnectionClientHandler());
        EncryptionConfig eConfig = createEncryptionConfig();
        eclient = new S3EncryptionClient(config, new URLConnectionClientHandler(), eConfig);
        encodeSpec = eConfig.getEncryptionSpec();
        if (eConfig.isCompressionEnabled()) encodeSpec = eConfig.getCompressionSpec() + "," + encodeSpec;
        return eclient;
    }

    @Override
    @Test
    public void testRetries() throws Exception {
        byte[] data = "Testing retries!!".getBytes();
        String key = "retry-test";

        S3Config _config = createS3Config().withUseV2Signer(false);
        _config.setFaultInjectionRate(0.4f);
        _config.setRetryLimit(6);
        S3Client _client = new S3EncryptionClient(_config, createEncryptionConfig());

        // make sure we hit at least one error
        for (int i = 0; i < 6; i++) {
            _client.putObject(getTestBucket(), key, data, null);
            S3ObjectMetadata metadata = rclient.getObjectMetadata(getTestBucket(), key);
            Assert.assertEquals(encodeSpec, metadata.getUserMetadata(CodecChain.META_TRANSFORM_MODE));
        }
    }
}
