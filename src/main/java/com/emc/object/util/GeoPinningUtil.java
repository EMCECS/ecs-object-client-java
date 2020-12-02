package com.emc.object.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;

public final class GeoPinningUtil {
    /**
     * If this is a bucket request, the bucket is the ID.
     * If this is an object request, the key is the ID.
     */
    public static String getGeoId(String bucketName, String objectKey) {
        if (objectKey == null || objectKey.length() == 0) return bucketName;

        return objectKey;
    }

    public static int getGeoPinIndex(String guid, int vdcCount) {
        // first 3 bytes of SHA1 hash modulus the number of VDCs
        byte[] sha1 = DigestUtils.sha1(guid);
        return ByteBuffer.wrap(new byte[]{0, sha1[0], sha1[1], sha1[2]}).getInt() % vdcCount;
    }

    private GeoPinningUtil() {
    }
}
