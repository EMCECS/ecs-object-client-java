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
package com.emc.object.s3.jersey;

import com.emc.object.ObjectConfig;
import com.emc.rest.smart.ecs.Vdc;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: this filter must be applied *before* the BucketFilter (it does not remove the bucket from
 * the path to extract the object key)
 */
public class GeoPinningFilter extends ClientFilter {
    private ObjectConfig<?> objectConfig;

    public GeoPinningFilter(ObjectConfig<?> objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected String getRequestGuid(ClientRequest cr) {
        String key = cr.getURI().getPath();
        if (key.startsWith("/")) key = key.substring(1);
        return key;
    }

    protected int getGeoPinIndex(String guid, int vdcCount) {
        // first 3 bytes of SHA1 hash modulus the number of VDCs
        byte[] sha1 = DigestUtils.sha1(guid);
        return ByteBuffer.wrap(new byte[]{0, sha1[0], sha1[1], sha1[2]}).getInt() % vdcCount;
    }

    @Override
    public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
        List<Vdc> healthyVdcs = new ArrayList<Vdc>();

        for (Vdc vdc : objectConfig.getVdcs()) {
            if (vdc.isHealthy()) healthyVdcs.add(vdc);
        }

        int geoPinIndex = getGeoPinIndex(getRequestGuid(cr), healthyVdcs.size());

        cr.getProperties().put(GeoPinningRule.PROP_GEO_PINNED_VDC, healthyVdcs.get(geoPinIndex));

        return getNext().handle(cr);
    }

    public ObjectConfig getObjectConfig() {
        return objectConfig;
    }
}
