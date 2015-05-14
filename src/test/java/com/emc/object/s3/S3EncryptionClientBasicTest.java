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

import com.emc.object.EncryptionConfig;
import com.emc.object.s3.bean.GetObjectResult;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.request.GetObjectRequest;
import com.emc.vipr.transform.TransformConstants;
import com.emc.vipr.transform.encryption.BasicEncryptionTransformFactory;
import com.emc.vipr.transform.encryption.KeyUtils;
import org.apache.log4j.LogMF;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class S3EncryptionClientBasicTest extends S3EncryptionClientKeyStoreTest {
    private static final Logger l4j = Logger.getLogger(S3EncryptionClientBasicTest.class);

    private String keyFile = "keys.properties";
    private KeyPair _masterKey;
    private KeyPair _oldKey;

    @Override
    protected EncryptionConfig createEncryptionConfig() throws Exception {
        return new EncryptionConfig(getMasterKey(), new HashSet<KeyPair>(Arrays.asList(getMasterKey(), getOldKey())), null, keySize);
    }

    @Override
    protected String getMasterKeyFingerprint() throws Exception {
        return KeyUtils.getRsaPublicKeyFingerprint((RSAPublicKey) getMasterKey().getPublic(), null);
    }

    protected synchronized KeyPair getMasterKey() throws Exception {
        if (_masterKey == null) loadKeys();
        return _masterKey;
    }

    protected synchronized KeyPair getOldKey() throws Exception {
        if (_oldKey == null) loadKeys();
        return _oldKey;
    }

    protected void loadKeys() throws Exception {
        Properties keyprops = new Properties();
        InputStream keystream = getClass().getClassLoader().getResourceAsStream(keyFile);
        Assume.assumeNotNull(keystream);
        keyprops.load(keystream);

        _masterKey = KeyUtils.rsaKeyPairFromBase64(keyprops.getProperty("masterkey.public"), keyprops.getProperty("masterkey.private"));
        LogMF.debug(l4j, "Master key sizes: public: {} private: {}",
                ((RSAPublicKey) _masterKey.getPublic()).getModulus().bitLength(),
                ((RSAPrivateKey) _masterKey.getPrivate()).getModulus().bitLength());
        _oldKey = KeyUtils.rsaKeyPairFromBase64(keyprops.getProperty("oldkey.public"), keyprops.getProperty("oldkey.private"));
        LogMF.debug(l4j, "Old key sizes: public: {} private: {}",
                ((RSAPublicKey) _oldKey.getPublic()).getModulus().bitLength(),
                ((RSAPrivateKey) _oldKey.getPrivate()).getModulus().bitLength());
    }

    @Override
    @Test
    public void testRekey() throws Exception {
        String key = "rekey-test.txt";
        String content = "Hello World!";

        client.putObject(getTestBucket(), key, content, null);

        // shouldn't need to rekey as the master key has not changed
        Assert.assertFalse(eclient.rekey(getTestBucket(), key));

        // change master key
        EncryptionConfig encryptionConfig = createEncryptionConfig();
        ((BasicEncryptionTransformFactory) encryptionConfig.getFactory()).setMasterEncryptionKey(getOldKey());
        S3EncryptionClient eclient2 = new S3EncryptionClient(createS3Config(), encryptionConfig);

        // now actually rekey
        Assert.assertTrue(eclient2.rekey(getTestBucket(), key));

        // Read back and test
        GetObjectResult<String> result = client.getObject(new GetObjectRequest(getTestBucket(), key), String.class);

        assertEquals("Content differs", content, result.getObject());
        assertEquals("unencrypted size incorrect", "12",
                result.getObjectMetadata().getUserMetadata(TransformConstants.META_ENCRYPTION_UNENC_SIZE));
        assertEquals("encrypted size incorrect", 16, result.getObjectMetadata().getContentLength().longValue());
        assertEquals("unencrypted sha1 incorrect", "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                result.getObjectMetadata().getUserMetadata(TransformConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("master key ID incorrect", KeyUtils.getRsaPublicKeyFingerprint((RSAPublicKey) getOldKey().getPublic(), null),
                result.getObjectMetadata().getUserMetadata(TransformConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", result.getObjectMetadata().getUserMetadata(TransformConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", result.getObjectMetadata().getUserMetadata(TransformConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                result.getObjectMetadata().getUserMetadata(TransformConstants.META_ENCRYPTION_META_SIG));
    }
}
