package com.emc.object;

import com.emc.vipr.transform.TransformConstants;
import com.emc.vipr.transform.TransformException;
import com.emc.vipr.transform.encryption.*;

import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Map;
import java.util.Set;

/**
 * Creates an encryption configuration for use with an object transform client.
 * Both keystore keys and bare RSA KeyPairs are supported.
 */
public class EncryptionConfig {
    private EncryptionTransformFactory<BasicEncryptionOutputTransform, BasicEncryptionInputTransform> factory;

    public static String getEncryptionMode(Map<String, String> metadata) {
        String transformModes = metadata.get(TransformConstants.META_TRANSFORM_MODE);

        // During decode, we process transforms in reverse order.
        String[] modes = transformModes.split("\\|");
        for (int i = modes.length - 1; i >= 0; i--) {
            if (modes[i].startsWith(TransformConstants.ENCRYPTION_CLASS)) return modes[i];
        }

        return null;
    }

    public static void setEncryptionMode(Map<String, String> metadata, String encryptionMode) {
        metadata.put(TransformConstants.META_TRANSFORM_MODE, encryptionMode);
    }

    /**
     * Creates a new EncryptionConfig object that will retrieve keys from a Keystore
     * object.
     *
     * @param keystore          the Keystore containing the master encryption key and any
     *                          additional decryption key(s).
     * @param masterKeyPassword password for the master keys.  Note that this
     *                          implementation assumes that all master keys use the same password.
     * @param masterKeyAlias    name of the master encryption key in the Keystore object.
     * @param provider          (optional) if not-null, the Provider object to use for all
     *                          encryption operations.  If null, the default provider(s) will be used from your
     *                          java.security file.
     * @param keySize           size of encryption key to use, either 128 or 256.  Note that to use
     *                          256-bit AES keys, you will probably need the unlimited strength jurisdiction files
     *                          installed in your JRE.
     * @throws java.security.InvalidKeyException      if the master encryption key cannot be loaded.
     * @throws java.security.NoSuchAlgorithmException if the AES encryption algorithm is not available.
     * @throws javax.crypto.NoSuchPaddingException    if PKCS5Padding is not available.
     * @throws TransformException                     if some other error occurred initializing the encryption.
     */
    public EncryptionConfig(KeyStore keystore, char[] masterKeyPassword,
                            String masterKeyAlias, Provider provider, int keySize)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, TransformException {
        if (provider == null) {
            factory = new KeyStoreEncryptionFactory(keystore, masterKeyAlias, masterKeyPassword);
        } else {
            factory = new KeyStoreEncryptionFactory(keystore, masterKeyAlias, masterKeyPassword, provider);
        }
        factory.setEncryptionSettings(TransformConstants.DEFAULT_ENCRYPTION_TRANSFORM, keySize, provider);
    }

    /**
     * Creates a new EncryptionConfig object that uses bare KeyPair objects.
     *
     * @param masterEncryptionKey the KeyPair to use for encryption.
     * @param decryptionKeys      (optional) additional KeyPair objects available to
     *                            decrypt objects.
     * @param provider            (optional) if not-null, the Provider object to use for all
     *                            encryption operations.  If null, the default provider(s) will be used from your
     *                            java.security file.
     * @param keySize             size of encryption key to use, either 128 or 256.  Note that to use
     *                            256-bit AES keys, you will probably need the unlimited strength jurisdiction files
     *                            installed in your JRE.
     * @throws java.security.InvalidKeyException      if the master encryption key is not valid
     * @throws java.security.NoSuchAlgorithmException if the AES encryption algorithm is not available.
     * @throws javax.crypto.NoSuchPaddingException    if PKCS5Padding is not available.
     * @throws TransformException                     if some other error occurred initializing the encryption.
     */
    public EncryptionConfig(KeyPair masterEncryptionKey, Set<KeyPair> decryptionKeys,
                            Provider provider, int keySize)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, TransformException {

        if (provider == null) {
            factory = new BasicEncryptionTransformFactory(masterEncryptionKey, decryptionKeys);
        } else {
            factory = new BasicEncryptionTransformFactory(masterEncryptionKey, decryptionKeys, provider);
        }
        factory.setEncryptionSettings(TransformConstants.DEFAULT_ENCRYPTION_TRANSFORM, keySize, provider);
    }

    /**
     * Returns the configured EncryptionTransformFactory.
     *
     * @return the configured EncryptionTransformFactory.
     */
    public EncryptionTransformFactory<BasicEncryptionOutputTransform, BasicEncryptionInputTransform> getFactory() {
        return factory;
    }

}
