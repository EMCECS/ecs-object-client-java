/*
 * Copyright (c) 2015-2018, EMC Corporation.
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
package com.emc.object.s3.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum BucketPolicyAction {
    @XmlEnumValue("s3:*")
    All("s3:*"),
    @XmlEnumValue("s3:GetObject")
    GetObject("s3:GetObject"),
    @XmlEnumValue("s3:GetObjectVersion")
    GetObjectVersion("s3:GetObjectVersion"),
    @XmlEnumValue("s3:PutObject")
    PutObject("s3:PutObject"),
    @XmlEnumValue("s3:GetObjectAcl")
    GetObjectAcl("s3:GetObjectAcl"),
    @XmlEnumValue("s3:GetObjectVersionAcl")
    GetObjectVersionAcl("s3:GetObjectVersionAcl"),
    @XmlEnumValue("s3:PutObjectAcl")
    PutObjectAcl("s3:PutObjectAcl"),
    @XmlEnumValue("s3:PutObjectVersionAcl")
    PutObjectVersionAcl("s3:PutObjectVersionAcl"),
    @XmlEnumValue("s3:DeleteObject")
    DeleteObject("s3:DeleteObject"),
    @XmlEnumValue("s3:DeleteObjectVersion")
    DeleteObjectVersion("s3:DeleteObjectVersion"),
    @XmlEnumValue("s3:ListMultipartUploadParts")
    ListMultipartUploadParts("s3:ListMultipartUploadParts"),
    @XmlEnumValue("s3:AbortMultipartUpload")
    AbortMultipartUpload("s3:AbortMultipartUpload"),
    @XmlEnumValue("s3:DeleteBucket")
    DeleteBucket("s3:DeleteBucket"),
    @XmlEnumValue("s3:ListBucket")
    ListBucket("s3:ListBucket"),
    @XmlEnumValue("s3:ListBucketVersions")
    ListBucketVersions("s3:ListBucketVersions"),
    @XmlEnumValue("s3:GetLifecycleConfiguration")
    GetLifecycleConfiguration("s3:GetLifecycleConfiguration"),
    @XmlEnumValue("s3:PutLifecycleConfiguration")
    PutLifecycleConfiguration("s3:PutLifecycleConfiguration"),
    @XmlEnumValue("s3:GetBucketAcl")
    GetBucketAcl("s3:GetBucketAcl"),
    @XmlEnumValue("s3:PutBucketAcl")
    PutBucketAcl("s3:PutBucketAcl"),
    @XmlEnumValue("s3:GetBucketCORS")
    GetBucketCORS("s3:GetBucketCORS"),
    @XmlEnumValue("s3:PutBucketCORS")
    PutBucketCORS("s3:PutBucketCORS"),
    @XmlEnumValue("s3:GetBucketVersioning")
    GetBucketVersioning("s3:GetBucketVersioning"),
    @XmlEnumValue("s3:PutBucketVersioning")
    PutBucketVersioning("s3:PutBucketVersioning"),
    @XmlEnumValue("s3:GetBucketPolicy")
    GetBucketPolicy("s3:GetBucketPolicy"),
    @XmlEnumValue("s3:DeleteBucketPolicy")
    DeleteBucketPolicy("s3:DeleteBucketPolicy"),
    @XmlEnumValue("s3:PutBucketPolicy")
    PutBucketPolicy("s3:PutBucketPolicy"),
    @XmlEnumValue("s3:PutBucketObjectLockConfiguration")
    PutBucketObjectLockConfiguration("s3:PutBucketObjectLockConfiguration"),
    @XmlEnumValue("s3:GetBucketObjectLockConfiguration")
    GetBucketObjectLockConfiguration("s3:GetBucketObjectLockConfiguration"),
    @XmlEnumValue("s3:PutObjectLegalHold")
    PutObjectLegalHold("s3:PutObjectLegalHold"),
    @XmlEnumValue("s3:GetObjectLegalHold")
    GetObjectLegalHold("s3:GetObjectLegalHold"),
    @XmlEnumValue("s3:PutObjectRetention")
    PutObjectRetention("s3:PutObjectRetention"),
    @XmlEnumValue("s3:GetObjectRetention")
    GetObjectRetention("s3:GetObjectRetention"),
    @XmlEnumValue("s3:BypassGovernanceRetention")
    BypassGovernanceRetention("s3:BypassGovernanceRetention"),
    @XmlEnumValue("s3:EnableObjectLock")
    EnableObjectLock("s3:EnableObjectLock"),
    @XmlEnumValue("s3:PutObjectTagging")
    PutObjectTagging("s3:PutObjectTagging"),
    @XmlEnumValue("s3:PutObjectVersionTagging")
    PutObjectVersionTagging("s3:PutObjectVersionTagging");

    private String actionName;

    BucketPolicyAction(String actionName) {
        this.actionName = actionName;
    }

    @JsonCreator
    public static BucketPolicyAction fromValue(String value) {
        for (BucketPolicyAction instance : values()) {
            if (value.equals(instance.getActionName())) return instance;
        }
        return null;
    }

    @JsonValue
    public String getActionName() {
        return actionName;
    }

    @Override
    public String toString() {
        return getActionName();
    }
}
