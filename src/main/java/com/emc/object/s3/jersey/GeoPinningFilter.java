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
import com.emc.object.util.GeoPinningUtil;
import com.emc.rest.smart.ecs.Vdc;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: this filter must be applied *before* the BucketFilter (it does not remove the bucket from
 * the path to extract the object key)
 */
@Provider
@Priority(FilterPriorities.PRIORITY_GEOPINNING)
public class GeoPinningFilter implements ClientRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GeoPinningFilter.class);

    private final ObjectConfig<?> objectConfig;

    public GeoPinningFilter(ObjectConfig<?> objectConfig) {
        this.objectConfig = objectConfig;
    }

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        // if there's no bucket, we don't need to pin the request (there's no write or read)
        Configuration configuration = request.getConfiguration();
        String bucketName = (String) configuration.getProperty(S3Constants.PROPERTY_BUCKET_NAME);
        String objectKey = (String) configuration.getProperty(S3Constants.PROPERTY_OBJECT_KEY);
        if (bucketName != null) {
            List<Vdc> healthyVdcs = new ArrayList<>();

            for (Vdc vdc : objectConfig.getVdcs()) {
                if (vdc.isHealthy()) healthyVdcs.add(vdc);
            }

            if (healthyVdcs.isEmpty()) {
                log.debug("there are no healthy VDCs; geo-pinning will include all VDCs");
                healthyVdcs.addAll(objectConfig.getVdcs());
            }

            int geoPinIndex = GeoPinningUtil.getGeoPinIndex(GeoPinningUtil.getGeoId(bucketName, objectKey), healthyVdcs.size());

            // if this is a read and failover for retries is requested, round-robin the VDCs for each retry
            if (objectConfig.isGeoReadRetryFailover() && Method.GET.name().equalsIgnoreCase(request.getMethod())) {
                Integer retries = (Integer) configuration.getProperty(ObjectConfig.PROPERTY_RETRY_COUNT);
                if (retries != null) {
                    int newIndex = (geoPinIndex + retries) % healthyVdcs.size();
                    log.info("geo-pin read retry #{}: failing over from primary VDC {} to VDC {}",
                            retries, geoPinIndex, newIndex);
                    geoPinIndex = newIndex;
                }
            }

            request.getConfiguration().getProperties().put(GeoPinningRule.PROP_GEO_PINNED_VDC, healthyVdcs.get(geoPinIndex));
        }

    }

    public ObjectConfig<?> getObjectConfig() {
        return objectConfig;
    }
}
