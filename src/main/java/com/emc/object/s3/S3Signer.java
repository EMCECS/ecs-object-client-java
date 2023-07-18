package com.emc.object.s3;

import com.emc.object.s3.request.PresignedUrlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.ClientRequestContext;
import javax.xml.bind.DatatypeConverter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class S3Signer {
    protected static final Logger log = LoggerFactory.getLogger(S3Signer.class);

    protected S3Config s3Config;

    S3Signer(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    /**
     * Sign the request
     */
    public abstract void sign(ClientRequestContext request, String resource, Map<String, String> parameters,
                              Map<String, List<Object>> headers);

    /**
     * Get the signature as String, singingKey is only
     * needed for v4 signer
     */
    protected abstract String getSignature(String stringToSign, byte[] signingKey);

    /**
     * Get the date as String
     */
    protected abstract String getDate(Map<String, String> parameters, Map<String, List<Object>> headers);

    /**
     * Generate presigned URL and then return the URL
     */
    public abstract URL generatePresignedUrl(PresignedUrlRequest request);

    protected abstract SortedMap<String, String> getCanonicalizedHeaders(Map<String, List<Object>> headers,
                                                              Map<String, String> parameters);


    // generalized utility function to get hmac values
    protected byte[] hmac(String algorithm, byte[] secretKey, String message) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secretKey, algorithm));
            byte[] result = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            log.debug("hmac of {} and {}:\n{}", secretKey, message, result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + " algorithm is not supported on this platform", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("The secret key is not valid", e);
        }
    }

    protected static byte[] hash256(String stringToHash) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(S3Constants.SHA256);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
        return hash;
    }


    /* *
     * encode byte string to hex - required for v4 auth
     * */
    protected static String hexEncode(byte[] arg) {
        String hexString = DatatypeConverter.printHexBinary(arg);
        if (hexString != null) {
            // return value of DatatypeConverter.printHexBinary is uppercase,
            // need to convert to lowercase
            return hexString.toLowerCase();
        } else {
            return null;
        }
    }

    protected String trimAndJoin(List<Object> values, String delimiter) {
        if (values == null || values.isEmpty()) return null;
        StringBuilder delimited = new StringBuilder();
        Iterator<Object> valuesI = values.iterator();
        while (valuesI.hasNext()) {
            delimited.append(valuesI.next().toString().trim());
            if (valuesI.hasNext()) delimited.append(delimiter);
        }
        return delimited.toString();
    }
}