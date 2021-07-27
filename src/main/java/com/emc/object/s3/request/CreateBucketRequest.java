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
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.s3.bean.MetadataSearchKey;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates parameters for a create bucket request.
 */
public class CreateBucketRequest extends AbstractBucketRequest {
    private CannedAcl cannedAcl;
    private AccessControlList acl;
    private String vPoolId;
    private Boolean fileSystemEnabled;
    private Boolean staleReadAllowed;
    private Boolean encryptionEnabled;
    private Long retentionPeriod;
    private String metadataSearchKeys;
    private boolean objectLockEnabled = false;

    public CreateBucketRequest(String bucketName) {
        super(Method.PUT, bucketName, "", null);
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();

        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        if (acl != null) headers.putAll(acl.toHeaders());
        if (vPoolId != null) RestUtil.putSingle(headers, RestUtil.EMC_VPOOL, vPoolId);
        if (fileSystemEnabled != null) RestUtil.putSingle(headers, RestUtil.EMC_FS_ENABLED, fileSystemEnabled);
        if (staleReadAllowed != null) RestUtil.putSingle(headers, RestUtil.EMC_STALE_READ_ALLOWED, staleReadAllowed);
        if (encryptionEnabled != null) RestUtil.putSingle(headers, RestUtil.EMC_ENCRYPTION_ENABLED, encryptionEnabled);
        if (retentionPeriod != null) RestUtil.putSingle(headers, RestUtil.EMC_RETENTION_PERIOD, retentionPeriod);
        if (metadataSearchKeys != null) RestUtil.putSingle(headers, RestUtil.EMC_METADATA_SEARCH, metadataSearchKeys);
        if (objectLockEnabled) RestUtil.putSingle(headers, S3Constants.AMZ_BUCKET_OBJECT_LOCK_ENABLED, objectLockEnabled);

        return headers;
    }

    public CannedAcl getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(CannedAcl cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    public String getvPoolId() {
        return vPoolId;
    }

    public void setvPoolId(String vPoolId) {
        this.vPoolId = vPoolId;
    }

    public Boolean getFileSystemEnabled() {
        return fileSystemEnabled;
    }

    /**
     * Sets whether the bucket can be access via filesystem (i.e. HDFS). This will enable some internal semantics for
     * directories and may affect other features (i.e.
     * {@link com.emc.object.s3.S3Client#setBucketStaleReadAllowed(String, boolean) TSO support})
     */
    public void setFileSystemEnabled(Boolean fileSystemEnabled) {
        this.fileSystemEnabled = fileSystemEnabled;
    }

    public Boolean getStaleReadAllowed() {
        return staleReadAllowed;
    }

    /**
     * Sets whether stale reads are allowed on the bucket
     *
     * @see com.emc.object.s3.S3Client#setBucketStaleReadAllowed(String, boolean)
     */
    public void setStaleReadAllowed(Boolean staleReadAllowed) {
        this.staleReadAllowed = staleReadAllowed;
    }

    public Boolean getEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * Enables transparent server-side encryption (D@RE) on the bucket. This can only be enabled at create time
     */
    public void setEncryptionEnabled(Boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public Long getRetentionPeriod() {
        return retentionPeriod;
    }

    /**
     * Enables a default retention period that will be applied to all objects created in the bucket
     *
     * @param retentionPeriod The default number of seconds each object will be in retention after creation
     */
    public void setRetentionPeriod(Long retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    /**
     * Assigns a list of system- and user-metadata keynames that can be used later in bucket searches
     * for the purpose of filtering object lists based querying these keys.
     *
     * @param metadataSearchKeys The set of keys to index.
     */
    public void setMetadataSearchKeys(List<MetadataSearchKey> metadataSearchKeys) {
        StringBuilder sb = new StringBuilder();
        for(MetadataSearchKey key : metadataSearchKeys) {
            if(sb.length() > 0) sb.append(',');
            sb.append(key.getName()).append(';').append(key.getDatatype());
        }
        this.metadataSearchKeys = sb.toString();
    }

    public boolean getObjectLockEnabled() { return objectLockEnabled; }

    /**
     * Sets whether S3 Object Lock will be enabled for the new bucket.
     */
    public void setObjectLockEnabled(boolean objectLockEnabled) {
        this.objectLockEnabled = objectLockEnabled;
    }

    public CreateBucketRequest withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }

    public CreateBucketRequest withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public CreateBucketRequest withVPoolId(String vPoolId) {
        setvPoolId(vPoolId);
        return this;
    }

    public CreateBucketRequest withFileSystemEnabled(boolean fileSystemEnabled) {
        setFileSystemEnabled(fileSystemEnabled);
        return this;
    }

    public CreateBucketRequest withStaleReadAllowed(boolean staleReadAllowed) {
        setStaleReadAllowed(staleReadAllowed);
        return this;
    }

    public CreateBucketRequest withEncryptionEnabled(Boolean encryptionEnabled) {
        setEncryptionEnabled(encryptionEnabled);
        return this;
    }

    public CreateBucketRequest withRetentionPeriod(long retentionPeriod) {
        setRetentionPeriod(retentionPeriod);
        return this;
    }

    public CreateBucketRequest withMetadataSearchKeys(List<MetadataSearchKey> metadataSearchKeys) {
        setMetadataSearchKeys(metadataSearchKeys);
        return this;
    }

    public CreateBucketRequest withObjectLockEnabled(boolean objectLockEnabled) {
        setObjectLockEnabled(objectLockEnabled);
        return this;
    }
}
