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
import com.emc.object.Range;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.util.RestUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PutObjectRequest extends S3ObjectRequest implements EntityRequest {
    private S3ObjectMetadata objectMetadata;
    private Object object;
    private Range range;
    private Date ifModifiedSince;
    private Date ifUnmodifiedSince;
    private String ifMatch;
    private String ifNoneMatch;
    private AccessControlList acl;
    private CannedAcl cannedAcl;

    public PutObjectRequest(String bucketName, String key, Object object) {
        super(Method.PUT, bucketName, key, null);
        this.object = object;
    }

    public PutObjectRequest(PutObjectRequest other) {
        super(other);
        this.objectMetadata = other.objectMetadata;
        this.object = other.object;
        this.range = other.range;
        this.acl = other.acl;
        this.cannedAcl = other.cannedAcl;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (range != null) RestUtil.putSingle(headers, RestUtil.HEADER_RANGE, "bytes=" + range.toString());
        if (objectMetadata != null) headers.putAll(objectMetadata.toHeaders());
        if (ifModifiedSince != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_IF_MODIFIED_SINCE, RestUtil.headerFormat(ifModifiedSince));
        if (ifUnmodifiedSince != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_IF_UNMODIFIED_SINE, RestUtil.headerFormat(ifUnmodifiedSince));
        if (ifMatch != null) RestUtil.putSingle(headers, RestUtil.HEADER_IF_MATCH, ifMatch);
        if (ifNoneMatch != null) RestUtil.putSingle(headers, RestUtil.HEADER_IF_NONE_MATCH, ifNoneMatch);
        if (acl != null) headers.putAll(acl.toHeaders());
        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        return headers;
    }

    @Override
    public Object getEntity() {
        return getObject();
    }

    @Override
    public String getContentType() {
        return objectMetadata != null ? objectMetadata.getContentType() : null;
    }

    @Override
    public Long getContentLength() {
        return objectMetadata != null ? objectMetadata.getContentLength() : null;
    }

    @Override
    public boolean isChunkable() {
        return true;
    }

    public S3ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setObjectMetadata(S3ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    public Object getObject() {
        return object;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Date getIfModifiedSince() {
        return ifModifiedSince;
    }

    public void setIfModifiedSince(Date ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
    }

    public Date getIfUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

    public void setIfUnmodifiedSince(Date ifUnmodifiedSince) {
        this.ifUnmodifiedSince = ifUnmodifiedSince;
    }

    public String getIfMatch() {
        return ifMatch;
    }

    public void setIfMatch(String ifMatch) {
        this.ifMatch = ifMatch;
    }

    public String getIfNoneMatch() {
        return ifNoneMatch;
    }

    public void setIfNoneMatch(String ifNoneMatch) {
        this.ifNoneMatch = ifNoneMatch;
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

    /**
     * @deprecated Use the method <code>com.emc.object.s3.S3ObjectMetadata.getRetentionPeriod</code> in preference to this one.
     * @return The retention period in seconds.
     */
    @Deprecated
    public Long getRetentionPeriod() {
        return (objectMetadata == null) ? null : objectMetadata.getRetentionPeriod();
    }

    /**
     * Sets the retention (read-only) period for the object in seconds (after <code>retentionPeriod</code> seconds,
     * you can modify or delete the object)
     * @deprecated Use the method <code>com.emc.object.s3.S3ObjectMetadata.setRetentionPeriod</code> in preference to this one.
     */
    @Deprecated
    public void setRetentionPeriod(Long retentionPeriod) {
        if (objectMetadata == null) {
            objectMetadata = new S3ObjectMetadata();
        }
        objectMetadata.setRetentionPeriod(retentionPeriod);
    }

    /**
     * @deprecated Use the method <code>com.emc.object.s3.S3ObjectMetadata.getRetentionPolicy</code> in preference to this one.
     * @return The retention policy name.
     */
    @Deprecated
    public String getRetentionPolicy() {
        return (objectMetadata == null) ? null : objectMetadata.getRetentionPolicy();
    }

    /**
     * Sets the name of the retention policy to apply to the object. Retention policies are defined within each
     * namespace
     * @deprecated Use the method <code>com.emc.object.s3.S3ObjectMetadata.setRetentionPolicy</code> in preference to this one.
     */
    @Deprecated
    public void setRetentionPolicy(String retentionPolicy) {
        if (objectMetadata == null) {
            objectMetadata = new S3ObjectMetadata();
        }
        objectMetadata.setRetentionPolicy(retentionPolicy);
    }

    public PutObjectRequest withObjectMetadata(S3ObjectMetadata objectMetadata) {
        setObjectMetadata(objectMetadata);
        return this;
    }

    public PutObjectRequest withRange(Range range) {
        setRange(range);
        return this;
    }

    public PutObjectRequest withIfModifiedSince(Date ifModifiedSince) {
        setIfModifiedSince(ifModifiedSince);
        return this;
    }

    public PutObjectRequest withIfUnmodifiedSince(Date ifUnmodifiedSince) {
        setIfUnmodifiedSince(ifUnmodifiedSince);
        return this;
    }

    public PutObjectRequest withIfMatch(String ifMatch) {
        setIfMatch(ifMatch);
        return this;
    }

    public PutObjectRequest withIfNoneMatch(String ifNoneMatch) {
        setIfNoneMatch(ifNoneMatch);
        return this;
    }

    public PutObjectRequest withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public PutObjectRequest withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }

    /**
     * Convenience method.
     * @deprecated Use the method <code>com.emc.object.s3.S3ObjectMetadata.setRetentionPeriod</code> in preference to this one.
     * @param retentionPeriod
     * @return The request.
     */
    @Deprecated
    public PutObjectRequest withRetentionPeriod(long retentionPeriod) {
        setRetentionPeriod(retentionPeriod);
        return this;
    }

    /**
     * Convenience method.
     * @deprecated Use the method <code>com.emc.object.s3.S3ObjectMetadata.setRetentionPolicy</code> in preference to this one.
     * @param retentionPolicy
     * @return The request.
     */
    @Deprecated
    public PutObjectRequest withRetentionPolicy(String retentionPolicy) {
        setRetentionPolicy(retentionPolicy);
        return this;
    }
}
