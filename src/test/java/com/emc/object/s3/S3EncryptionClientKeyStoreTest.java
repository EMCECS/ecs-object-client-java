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

import com.emc.codec.encryption.BasicKeyProvider;
import com.emc.codec.encryption.EncryptionConstants;
import com.emc.codec.encryption.EncryptionUtil;
import com.emc.codec.encryption.KeystoreKeyProvider;
import com.emc.object.EncryptionConfig;
import com.emc.object.s3.jersey.S3EncryptionClient;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;

public class S3EncryptionClientKeyStoreTest extends S3EncryptionClientBasicTest {
    private static final Logger l4j = Logger.getLogger(S3JerseyClientTest.class);

    private String keyAlias = "masterkey";
    private String oldKeyAlias = "oldkey";
    private BasicKeyProvider _keyProvider;

    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-client-keystore-test";
    }

    @Override
    protected synchronized BasicKeyProvider getKeyProvider() throws Exception {
        if (_keyProvider == null) {
            String keystoreFile = "keystore.jks";
            String keystorePassword = "viprviprvipr";

            KeyStore keyStore = KeyStore.getInstance("jks");
            InputStream in = this.getClass().getClassLoader().getResourceAsStream(keystoreFile);
            if (in == null) throw new FileNotFoundException(keystoreFile);
            keyStore.load(in, keystorePassword.toCharArray());

            l4j.debug("Keystore Loaded");
            for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements(); ) {
                l4j.debug("Found key: " + aliases.nextElement());
            }

            _keyProvider = new KeystoreKeyProvider(keyStore, keystorePassword.toCharArray(), keyAlias);
        }
        return _keyProvider;
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
        ((KeystoreKeyProvider) getKeyProvider()).setMasterKeyAlias(oldKeyAlias);
        S3EncryptionClient eclient2 = new S3EncryptionClient(createS3Config(), encryptionConfig);

        // now actually rekey
        Assert.assertTrue(eclient2.rekey(getTestBucket(), key));

        // Read back and test
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        KeyPair oldKeyPair = ((KeystoreKeyProvider) getKeyProvider()).getKeyFromAlias(oldKeyAlias);
        String oldKeyFingerprint = EncryptionUtil.getRsaPublicKeyFingerprint((RSAPublicKey) oldKeyPair.getPublic());
        assertEquals("Content differs", content, client.readObject(getTestBucket(), key, String.class));
        assertEquals("unencrypted size incorrect", "12",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE));
        assertEquals("encrypted size incorrect", 16, objectMetadata.getContentLength().longValue());
        assertEquals("unencrypted sha1 incorrect", "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("master key ID incorrect", oldKeyFingerprint,
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_META_SIG));
    }
}
