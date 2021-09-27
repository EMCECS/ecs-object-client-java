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
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class S3EncryptionClientBasicV4Test extends S3EncryptionClientBasicTest {
    private static final Logger l4j = Logger.getLogger(S3EncryptionClientBasicV4Test.class);

    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-client-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        rclient = new S3JerseyClient(createS3Config().withUseV2Signer(false));
        EncryptionConfig eConfig = createEncryptionConfig();
        eclient = new S3EncryptionClient(createS3Config().withUseV2Signer(false), eConfig);
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
