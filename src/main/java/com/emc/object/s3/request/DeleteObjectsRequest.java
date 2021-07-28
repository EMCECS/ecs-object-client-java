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
package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.bean.DeleteObjects;
import com.emc.object.s3.bean.ObjectKey;
import com.emc.object.util.RestUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DeleteObjectsRequest extends AbstractBucketRequest implements EntityRequest {
    private DeleteObjects deleteObjects;
    private Boolean bypassGovernanceRetention;

    public DeleteObjectsRequest(String bucketName) {
        super(Method.POST, bucketName, "", "delete");
        property(RestUtil.PROPERTY_GENERATE_CONTENT_MD5, Boolean.TRUE); // sign the MD5 to prevent replays
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (bypassGovernanceRetention != null) RestUtil.putSingle(headers, S3Constants.AMZ_OBJECT_LOCK_BYPASS_GOVERNANCE_RETENTION, bypassGovernanceRetention.toString());
        return headers;
    }

    @Override
    public Object getEntity() {
        return getDeleteObjects();
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    @Override
    public Long getContentLength() {
        return null; // assume chunked encoding or buffering
    }

    @Override
    public boolean isChunkable() {
        return false;
    }

    public DeleteObjects getDeleteObjects() {
        return deleteObjects;
    }

    public void setDeleteObjects(DeleteObjects deleteObjects) {
        this.deleteObjects = deleteObjects;
    }

    public synchronized DeleteObjectsRequest withKeys(List<ObjectKey> keys) {
        if (deleteObjects == null)
            deleteObjects = new DeleteObjects();
        deleteObjects.setKeys(keys);
        return this;
    }

    public DeleteObjectsRequest withKeys(ObjectKey... keys) {
        return withKeys(Arrays.asList(keys));
    }

    public DeleteObjectsRequest withKeys(String... keys) {
        ObjectKey[] objects = new ObjectKey[keys.length];
        for (int i = 0; i < keys.length; i++) {
            objects[i] = new ObjectKey(keys[i]);
        }
        return withKeys(objects);
    }

    public boolean getBypassGovernanceRetention() {
        return bypassGovernanceRetention;
    }

    public void  setBypassGovernanceRetention(Boolean bypassGovernanceRetention) {
        this.bypassGovernanceRetention = bypassGovernanceRetention;
    }

    public DeleteObjectsRequest withBypassGovernanceRetention(Boolean bypassGovernanceRetention) {
        setBypassGovernanceRetention(bypassGovernanceRetention);
        return this;
    }
}
