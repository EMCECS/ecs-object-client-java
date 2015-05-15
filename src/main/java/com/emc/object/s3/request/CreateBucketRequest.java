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
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class CreateBucketRequest extends AbstractBucketRequest {
    private CannedAcl cannedAcl;
    private AccessControlList acl;
    private String vPoolId;
    private Boolean fileSystemEnabled;

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

    public void setFileSystemEnabled(Boolean fileSystemEnabled) {
        this.fileSystemEnabled = fileSystemEnabled;
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

    public CreateBucketRequest withFileSystemEnabled(Boolean fileSystemEnabled) {
        setFileSystemEnabled(fileSystemEnabled);
        return this;
    }
}
