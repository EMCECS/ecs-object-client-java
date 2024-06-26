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

import com.emc.object.Range;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class CopyPartRequest extends CopyObjectRequest {
    private String uploadId;
    private int partNumber;
    private Range sourceRange;

    public CopyPartRequest(String sourceBucketName, String sourceKey, String bucketName, String key, String uploadId, int partNumber) {
        super(sourceBucketName, sourceKey, bucketName, key);
        this.uploadId = uploadId;
        this.partNumber = partNumber;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        queryParams.put(S3Constants.PARAM_UPLOAD_ID, uploadId);
        queryParams.put(S3Constants.PARAM_PART_NUMBER, Integer.toString(partNumber));
        return queryParams;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (sourceRange != null) RestUtil.putSingle(headers, S3Constants.AMZ_SOURCE_RANGE, "bytes=" + sourceRange);
        return headers;
    }

    public String getUploadId() {
        return uploadId;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public Range getSourceRange() {
        return sourceRange;
    }

    public void setSourceRange(Range sourceRange) {
        this.sourceRange = sourceRange;
    }

    public CopyPartRequest withSourceRange(Range sourceRange) {
        setSourceRange(sourceRange);
        return this;
    }

    @Override
    public CopyPartRequest withSourceVersionId(String sourceVersionId) {
        super.setSourceVersionId(sourceVersionId);
        return this;
    }
}
