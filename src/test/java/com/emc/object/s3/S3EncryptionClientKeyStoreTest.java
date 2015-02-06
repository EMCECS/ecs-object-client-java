package com.emc.object.s3;

import com.emc.object.EncryptionConfig;
import com.emc.object.s3.bean.GetObjectResult;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.request.GetObjectRequest;
import com.emc.util.RandomInputStream;
import com.emc.vipr.transform.TransformConstants;
import com.emc.vipr.transform.encryption.KeyStoreEncryptionFactory;
import com.emc.vipr.transform.encryption.KeyUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class S3EncryptionClientKeyStoreTest extends S3JerseyClientTest {
    private static final Logger l4j = Logger.getLogger(S3JerseyClientTest.class);

    protected int keySize = 128;
    protected S3EncryptionClient eclient;

    private String keystorePassword = "viprviprvipr";
    private String keyAlias = "masterkey";
    private String oldKeyAlias = "oldkey";
    private String keystoreFile = "keystore.jks";
    private KeyStore _keystore;

    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-client-test";
    }

    @Override
    public void initClient() throws Exception {
        client = eclient = new S3EncryptionClient(createS3Config(), createEncryptionConfig());
    }

    protected EncryptionConfig createEncryptionConfig() throws Exception {
        return new EncryptionConfig(getKeystore(), keystorePassword.toCharArray(), keyAlias, null, keySize);
    }

    protected String getMasterKeyFingerprint() throws Exception {
        return getKeyFingerprint(keyAlias);
    }

    private synchronized KeyStore getKeystore() throws Exception {
        if (_keystore == null) {
            _keystore = KeyStore.getInstance("jks");

            InputStream in = this.getClass().getClassLoader().getResourceAsStream(keystoreFile);
            if (in == null) throw new FileNotFoundException(keystoreFile);
            _keystore.load(in, keystorePassword.toCharArray());

            l4j.debug("Keystore Loaded");
            for (Enumeration<String> aliases = _keystore.aliases(); aliases.hasMoreElements(); ) {
                l4j.debug("Found key: " + aliases.nextElement());
            }
        }
        return _keystore;
    }

    protected String getKeyFingerprint(String keyAlias) throws Exception {
        return KeyUtils.getRsaPublicKeyFingerprint((RSAPublicKey) getKeystore().getCertificate(keyAlias).getPublicKey(), null);
    }

    @Test
    public void testEncryption() throws Exception {
        String key = "hello.txt";
        String content = "Hello World!";

        client.putObject(getTestBucket(), key, content, null);
        GetObjectResult<byte[]> result = client.getObject(new GetObjectRequest(getTestBucket(), key), byte[].class);

        Assert.assertEquals("unencrypted size incorrect", "12",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SIZE));
        Assert.assertEquals("encrypted size incorrect", "16", result.getObjectMetadata().userMetadata("size"));
        Assert.assertEquals("unencrypted sha1 incorrect", "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SHA1));
        Assert.assertEquals("master key ID incorrect", getMasterKeyFingerprint(),
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_META_SIG));
    }

    @Test
    public void testStream() throws Exception {
        String key = "test-file.txt";
        InputStream rawInput = getClass().getClassLoader().getResourceAsStream("uncompressed.txt");
        Assume.assumeNotNull(rawInput);

        client.putObject(getTestBucket(), key, rawInput, null);
        GetObjectResult<String> result = client.getObject(new GetObjectRequest(getTestBucket(), key), String.class);

        Assert.assertEquals("unencrypted size incorrect", "2516125",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SIZE));
        Assert.assertEquals("encrypted size incorrect", 2516128L, result.getObjectMetadata().getContentLength().longValue());
        Assert.assertEquals("unencrypted sha1 incorrect", "027e997e6b1dfc97b93eb28dc9a6804096d85873",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SHA1));
        Assert.assertEquals("master key ID incorrect", getMasterKeyFingerprint(),
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_META_SIG));
    }

    // Test a stream > 4MB.
    @Test
    public void testLargeStream() throws Exception {
        String key = "big-stream.obj";
        int size = 5 * 1024 * 1024 + 13;
        RandomInputStream rs = new RandomInputStream(size);

        client.putObject(getTestBucket(), key, rs, null);
        GetObjectResult<byte[]> result = client.getObject(new GetObjectRequest(getTestBucket(), key), byte[].class);

        // Make sure the checksum matches
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Digest = sha1.digest(result.getObject());

        // Hex Encode it
        String sha1hex = KeyUtils.toHexPadded(sha1Digest);

        assertNotNull("Missing SHA1 meta", result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("SHA1 incorrect", sha1hex,
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("Stream length incorrect", size,
                Integer.parseInt(result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SIZE)));
    }

    @Test
    public void testRekey() throws Exception {
        String key = "rekey-test.txt";
        String content = "Hello World!";

        client.putObject(getTestBucket(), key, content, null);

        // shouldn't need to rekey as the master key has not changed
        Assert.assertFalse(eclient.rekey(getTestBucket(), key));

        // change master key
        EncryptionConfig encryptionConfig = createEncryptionConfig();
        ((KeyStoreEncryptionFactory) encryptionConfig.getFactory()).setMasterEncryptionKeyAlias(oldKeyAlias);
        S3EncryptionClient eclient2 = new S3EncryptionClient(createS3Config(), encryptionConfig);

        // now actually rekey
        Assert.assertTrue(eclient2.rekey(getTestBucket(), key));

        // Read back and test
        GetObjectResult<String> result = client.getObject(new GetObjectRequest(getTestBucket(), key), String.class);

        assertEquals("Content differs", content, result.getObject());
        assertEquals("unencrypted size incorrect", "12",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SIZE));
        assertEquals("encrypted size incorrect", 16, result.getObjectMetadata().getContentLength().longValue());
        assertEquals("unencrypted sha1 incorrect", "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("master key ID incorrect", getKeyFingerprint(oldKeyAlias),
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                result.getObjectMetadata().userMetadata(TransformConstants.META_ENCRYPTION_META_SIG));
    }
}
