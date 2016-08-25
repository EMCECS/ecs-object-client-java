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

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.bean.EncodingType;

import java.util.Map;

public class ListMultipartUploadsRequest extends AbstractBucketRequest {
    private String prefix;
    private String delimiter;
    private Integer maxUploads;
    private String keyMarker;
    private String uploadIdMarker;
    private EncodingType encodingType;

    public ListMultipartUploadsRequest(String bucketName) {
        super(Method.GET, bucketName, "", "uploads");
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> paramMap = super.getQueryParams();
        if (prefix != null) paramMap.put(S3Constants.PARAM_PREFIX, prefix);
        if (delimiter != null) paramMap.put(S3Constants.PARAM_DELIMITER, delimiter);
        if (maxUploads != null) paramMap.put(S3Constants.PARAM_MAX_UPLOADS, maxUploads.toString());
        if (keyMarker != null) paramMap.put(S3Constants.PARAM_KEY_MARKER, keyMarker);
        if (uploadIdMarker != null) paramMap.put(S3Constants.PARAM_UPLOAD_ID_MARKER, uploadIdMarker);
        if (encodingType != null) paramMap.put(S3Constants.PARAM_ENCODING_TYPE, encodingType.toString());
        return paramMap;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public Integer getMaxUploads() {
        return maxUploads;
    }

    public void setMaxUploads(Integer maxUploads) {
        this.maxUploads = maxUploads;
    }

    public String getKeyMarker() {
        return keyMarker;
    }

    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    public String getUploadIdMarker() {
        return uploadIdMarker;
    }

    public void setUploadIdMarker(String uploadIdMarker) {
        this.uploadIdMarker = uploadIdMarker;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }

    public ListMultipartUploadsRequest withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    public ListMultipartUploadsRequest withDelimiter(String delimiter) {
        setDelimiter(delimiter);
        return this;
    }

    public ListMultipartUploadsRequest withMaxUploads(Integer maxUploads) {
        setMaxUploads(maxUploads);
        return this;
    }

    public ListMultipartUploadsRequest withKeyMarker(String keyMarker) {
        setKeyMarker(keyMarker);
        return this;
    }

    public ListMultipartUploadsRequest withUploadIdMarker(String uploadIdMarker) {
        setUploadIdMarker(uploadIdMarker);
        return this;
    }

    public ListMultipartUploadsRequest withEncodingType(EncodingType encodingType) {
        setEncodingType(encodingType);
        return this;
    }
}
