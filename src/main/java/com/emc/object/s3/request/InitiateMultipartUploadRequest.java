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
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.s3.bean.ObjectLockLegalHold;
import com.emc.object.s3.bean.ObjectLockRetention;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class InitiateMultipartUploadRequest extends S3ObjectRequest {
    private S3ObjectMetadata objectMetadata;
    private AccessControlList acl;
    private CannedAcl cannedAcl;
    private ObjectLockLegalHold objectLockLegalHold;
    private ObjectLockRetention objectLockRetention;

    public InitiateMultipartUploadRequest(String bucketName, String key) {
        super(Method.POST, bucketName, key, "uploads");
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (objectMetadata != null) headers.putAll(objectMetadata.toHeaders());
        if (acl != null) headers.putAll(acl.toHeaders());
        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        if (objectLockLegalHold != null) RestUtil.putSingle(headers, S3Constants.AMZ_OBJECT_LOCK_LEGAL_HOLD, objectLockLegalHold.getStatus());
        if (objectLockRetention != null) {
            RestUtil.putSingle(headers, S3Constants.AMZ_OBJECT_LOCK_MODE, objectLockRetention.getMode());
            if (objectLockRetention.getRetainUntilDate() != null) {
                RestUtil.putSingle(headers, S3Constants.AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE,
                        RestUtil.awsTimestampFormatter.format(objectLockRetention.getRetainUntilDate().toInstant()));
            }
        }
        return headers;
    }

    public S3ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setObjectMetadata(S3ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    public CannedAcl getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(CannedAcl cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    public ObjectLockLegalHold getObjectLockLegalHold() {
        return objectLockLegalHold;
    }

    public void setObjectLockLegalHold(ObjectLockLegalHold objectLockLegalHold) {
        this.objectLockLegalHold = objectLockLegalHold;
    }

    public ObjectLockRetention getObjectLockRetention() {
        return objectLockRetention;
    }

    public void setObjectLockRetention(ObjectLockRetention objectLockRetention) {
        this.objectLockRetention = objectLockRetention;
    }

    public InitiateMultipartUploadRequest withObjectMetadata(S3ObjectMetadata objectMetadata) {
        setObjectMetadata(objectMetadata);
        return this;
    }

    public InitiateMultipartUploadRequest withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public InitiateMultipartUploadRequest withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }

    public InitiateMultipartUploadRequest withObjectLockLegalHold(ObjectLockLegalHold objectLockLegalHold) {
        setObjectLockLegalHold(objectLockLegalHold);
        return this;
    }
    public InitiateMultipartUploadRequest withObjectLockRetention(ObjectLockRetention objectLockRetention) {
        setObjectLockRetention(objectLockRetention);
        return this;
    }
}
