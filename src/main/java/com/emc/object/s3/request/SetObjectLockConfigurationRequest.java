/*
 * Copyright (c) 2025, DELL Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of DELL Corporation may not be used to endorse or promote
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
import com.emc.object.s3.bean.ObjectLockConfiguration;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class SetObjectLockConfigurationRequest extends GenericBucketRequest implements EntityRequest{
    private ObjectLockConfiguration objectLockConfiguration;

    public SetObjectLockConfigurationRequest(String bucketName) {
        super(Method.PUT, bucketName,"object-lock");
        property(RestUtil.PROPERTY_GENERATE_CONTENT_MD5, Boolean.TRUE); // sign the MD5 to prevent replays
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        return headers;
    }

    @Override
    public ObjectLockConfiguration getEntity() {
        return this.objectLockConfiguration;
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public boolean isChunkable() {
        return false;
    }

    public void setObjectLockConfiguration(ObjectLockConfiguration objectLockConfiguration)
    {
        this.objectLockConfiguration = objectLockConfiguration;
    }

    public ObjectLockConfiguration getObjectLockConfiguration()
    {
        return this.objectLockConfiguration;
    }

    public SetObjectLockConfigurationRequest withObjectLockConfiguration(ObjectLockConfiguration objectLockConfiguration) {
        setObjectLockConfiguration(objectLockConfiguration);
        return this;
    }
}