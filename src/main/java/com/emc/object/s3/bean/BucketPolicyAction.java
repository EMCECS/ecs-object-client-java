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
package com.emc.object.s3.bean;

import com.emc.object.s3.S3Constants;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum BucketPolicyAction {
    GetObject("s3:GetObject"),
    GetObjectVersion("s3:GetObjectVersion"),
    PutObject("s3:PutObject"),
    GetObjectAcl("s3:GetObjectAcl"),
    GetObjectVersionAcl("s3:GetObjectVersionAcl"),
    PutObjectAcl("s3:PutObjectAcl"),
    PutObjectVersionAcl("s3:PutObjectVersionAcl"),
    DeleteObject("s3:DeleteObject"),
    DeleteObjectVersion("s3:DeleteObjectVersion"),
    ListMultipartUploadParts("s3:ListMultipartUploadParts"),
    AbortMultipartUpload("s3:AbortMultipartUpload"),
    DeleteBucket("s3:DeleteBucket"),
    ListBucket("s3:ListBucket"),
    ListBucketVersions("s3:ListBucketVersions"),
    GetLifecycleConfiguration("s3:GetLifecycleConfiguration"),
    PutLifecycleConfiguration("s3:PutLifecycleConfiguration"),
    GetBucketAcl("s3:GetBucketAcl"),
    PutBucketAcl("s3:PutBucketAcl"),
    GetBucketCORS("s3:GetBucketCORS"),
    PutBucketCORS("s3:PutBucketCORS"),
    GetBucketVersioning("s3:GetBucketVersioning"),
    PutBucketVersioning("s3:PutBucketVersioning"),
    GetBucketPolicy("s3:GetBucketPolicy"),
    DeleteBucketPolicy("s3:DeleteBucketPolicy"),
    PutBucketPolicy("s3:PutBucketPolicy");

    private String actionName;

    private BucketPolicyAction(String actionName) {
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }
}
