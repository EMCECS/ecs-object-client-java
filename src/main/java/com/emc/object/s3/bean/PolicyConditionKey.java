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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum PolicyConditionKey {
    @XmlEnumValue("aws:CurrentTime")
    CurrentTime("aws:CurrentTime"),
    @XmlEnumValue("aws:EpochTime")
    EpochTime("aws:EpochTime"),
    @XmlEnumValue("aws:principalType")
    PrincipalType("aws:principalType"),
    @XmlEnumValue("aws:SourceIp")
    SourceIp("aws:SourceIp"),
    @XmlEnumValue("aws:UserAgent")
    UserAgent("aws:UserAgent"),
    @XmlEnumValue("aws:username")
    UserName("aws:username"),
    @XmlEnumValue("s3:x-amz-acl")
    Acl("s3:x-amz-acl"),
    @XmlEnumValue("s3:x-amz-copy-source")
    CopySource("s3:x-amz-copy-source"),
    @XmlEnumValue("s3:x-amz-server-side-encryption")
    ServerSideEncryption("s3:x-amz-server-side-encryption"),
    @XmlEnumValue("s3:x-amz-server-side-encryption-aws-kms-key-id")
    ServerSideEncryptionAwsKmsKeyId("s3:x-amz-server-side-encryption-aws-kms-key-id"),
    @XmlEnumValue("s3:x-amz-metadata-directive")
    MetadataDirective("s3:x-amz-metadata-directive"),
    @XmlEnumValue("s3:x-amz-storage-class")
    StorageClass("s3:x-amz-storage-class"),
    @XmlEnumValue("s3:VersionId")
    VersionId("s3:VersionId"),
    @XmlEnumValue("s3:x-amz-grant-read")
    GrantReadPermission("s3:x-amz-grant-read"),
    @XmlEnumValue("s3:x-amz-grant-write")
    GrantWritePermission("s3:x-amz-grant-write"),
    @XmlEnumValue("s3:x-amz-grant-read-acp")
    GrantReadAcpPermission("s3:x-amz-grant-read-acp"),
    @XmlEnumValue("s3:x-amz-grant-write-acp")
    GrantWriteAcpPermission("s3:x-amz-grant-write-acp"),
    @XmlEnumValue("s3:x-amz-grant-full-control")
    GrantFullControlPermission("s3:x-amz-grant-full-control"),
    @XmlEnumValue("s3:prefix")
    Prefix("s3:prefix"),
    @XmlEnumValue("s3:delimiter")
    Delimiter("s3:delimiter"),
    @XmlEnumValue("s3:max-keys")
    MaxKeys("s3:max-keys");

    @JsonCreator
    public static PolicyConditionKey fromValue(String value) {
        for (PolicyConditionKey instance : values()) {
            if (value.equals(instance.getConditionKey())) return instance;
        }
        return null;
    }

    private String conditionKey;

    PolicyConditionKey(String conditionKey) {
        this.conditionKey = conditionKey;
    }

    @JsonValue
    public String getConditionKey() {
        return conditionKey;
    }

    @Override
    public String toString() {
        return getConditionKey();
    }
}
