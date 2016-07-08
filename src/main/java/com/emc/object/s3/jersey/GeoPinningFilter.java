/*
 * Copyright (c) 2015-2016, EMC Corporation.
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
package com.emc.object.s3.jersey;

import com.emc.object.Method;
import com.emc.object.ObjectConfig;
import com.emc.object.s3.S3Constants;
import com.emc.rest.smart.ecs.Vdc;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: this filter must be applied *before* the BucketFilter (it does not remove the bucket from
 * the path to extract the object key)
 */
public class GeoPinningFilter extends ClientFilter {

    private static final Logger log = LoggerFactory.getLogger(GeoPinningFilter.class);

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

    private ObjectConfig<?> objectConfig;

    public GeoPinningFilter(ObjectConfig<?> objectConfig) {
        this.objectConfig = objectConfig;
    }

    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        // if there's no bucket, we don't need to pin the request (there's no write or read)
        String bucketName = (String) request.getProperties().get(S3Constants.PROPERTY_BUCKET_NAME);
        String objectKey = (String) request.getProperties().get(S3Constants.PROPERTY_OBJECT_KEY);
        if (bucketName != null) {
            List<Vdc> healthyVdcs = new ArrayList<Vdc>();

            for (Vdc vdc : objectConfig.getVdcs()) {
                if (vdc.isHealthy()) healthyVdcs.add(vdc);
            }

            if (healthyVdcs.isEmpty()) {
                log.debug("there are no healthy VDCs; geo-pinning will include all VDCs");
                healthyVdcs.addAll(objectConfig.getVdcs());
            }

            int geoPinIndex = getGeoPinIndex(getGeoId(bucketName, objectKey), healthyVdcs.size());

            // if this is a read and failover for retries is requested, round-robin the VDCs for each retry
            if (objectConfig.isGeoReadRetryFailover() && Method.GET.name().equalsIgnoreCase(request.getMethod())) {
                Integer retries = (Integer) request.getProperties().get(RetryFilter.PROP_RETRY_COUNT);
                if (retries != null) {
                    int newIndex = (geoPinIndex + retries) % healthyVdcs.size();
                    log.info("geo-pin read retry #{}: failing over from primary VDC {} to VDC {}",
                            new Object[] { retries, geoPinIndex, newIndex });
                    geoPinIndex = newIndex;
                }
            }

            request.getProperties().put(GeoPinningRule.PROP_GEO_PINNED_VDC, healthyVdcs.get(geoPinIndex));
        }

        return getNext().handle(request);
    }

    public ObjectConfig getObjectConfig() {
        return objectConfig;
    }
}
