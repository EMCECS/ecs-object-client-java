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
import com.emc.object.s3.bean.CompleteMultipartUpload;
import com.emc.object.s3.bean.MultipartPartETag;
import com.emc.object.util.RestUtil;

import java.util.Map;
import java.util.SortedSet;

public class CompleteMultipartUploadRequest extends S3ObjectRequest implements EntityRequest<CompleteMultipartUpload> {
    private String uploadId;
    private SortedSet<MultipartPartETag> parts;

    public CompleteMultipartUploadRequest(String bucketName, String key, String uploadId) {
        super(Method.POST, bucketName, key, null);
        this.uploadId = uploadId;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        queryParams.put("uploadId", uploadId);
        return queryParams;
    }

    @Override
    public CompleteMultipartUpload getEntity() {
        return new CompleteMultipartUpload(parts);
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    @Override
    public Long getContentLength() {
        return null; // assume chunked encoding or buffering
    }

    public String getUploadId() {
        return uploadId;
    }

    public SortedSet<MultipartPartETag> getParts() {
        return parts;
    }

    public void setParts(SortedSet<MultipartPartETag> parts) {
        this.parts = parts;
    }

    public CompleteMultipartUploadRequest withParts(SortedSet<MultipartPartETag> parts) {
        setParts(parts);
        return this;
    }
}
