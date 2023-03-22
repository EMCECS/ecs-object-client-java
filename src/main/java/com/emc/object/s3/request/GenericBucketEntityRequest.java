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
import com.emc.object.util.RestUtil;

public class GenericBucketEntityRequest<T> extends GenericBucketRequest implements EntityRequest {
    private T entity;
    private String contentType;

    public GenericBucketEntityRequest(Method method, String bucketName, String subresource, T entity) {
        super(method, bucketName, subresource);
        this.entity = entity;
        property(RestUtil.PROPERTY_GENERATE_CONTENT_MD5, Boolean.TRUE); // sign the MD5 to prevent replays
    }

    @Override
    public T getEntity() {
        return entity;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Long getContentLength() {
        return null; // assume buffering
    }

    @Override
    public boolean isChunkable() {
        return false;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public GenericBucketEntityRequest withContentType(String contentType) {
        setContentType(contentType);
        return this;
    }
}
