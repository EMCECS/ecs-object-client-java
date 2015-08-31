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

import com.emc.object.s3.request.EncodingType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListPartsResult")
public class ListPartsResult {
    private String bucketName;
    private String key;
    private String uploadId;
    private String prefix;
    private String delimiter;
    private Integer maxParts;
    private EncodingType encodingType;
    private String partNumberMarker;
    private String nextPartNumberMarker;
    private boolean truncated;
    private CanonicalUser initiator;
    private CanonicalUser owner;
    private StorageClass storageClass;
    private List<MultipartPart> parts = new ArrayList<MultipartPart>();

    @XmlElement(name = "Bucket")
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @XmlElement(name = "Key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlElement(name = "UploadId")
    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    @XmlElement(name = "Prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @XmlElement(name = "Delimiter")
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @XmlElement(name = "MaxParts")
    public Integer getMaxParts() {
        return maxParts;
    }

    public void setMaxParts(Integer maxParts) {
        this.maxParts = maxParts;
    }

    @XmlElement(name = "Encoding-Type")
    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }

    @XmlElement(name = "PartNumberMarker")
    public String getPartNumberMarker() {
        return partNumberMarker;
    }

    public void setPartNumberMarker(String partNumberMarker) {
        this.partNumberMarker = partNumberMarker;
    }

    @XmlElement(name = "NextPartNumberMarker")
    public String getNextPartNumberMarker() {
        return nextPartNumberMarker;
    }

    public void setNextPartNumberMarker(String nextPartNumberMarker) {
        this.nextPartNumberMarker = nextPartNumberMarker;
    }

    /**
     * @deprecated (2.0.4) use {@link #isTruncated()} instead
     */
    @XmlTransient
    public Boolean getTruncated() {
        return isTruncated();
    }

    @XmlElement(name = "IsTruncated")
    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    @XmlElement(name = "Initiator")
    public CanonicalUser getInitiator() {
        return initiator;
    }

    public void setInitiator(CanonicalUser initiator) {
        this.initiator = initiator;
    }

    @XmlElement(name = "Owner")
    public CanonicalUser getOwner() {
        return owner;
    }

    public void setOwner(CanonicalUser owner) {
        this.owner = owner;
    }

    @XmlElement(name = "StorageClass")
    public StorageClass getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(StorageClass storageClass) {
        this.storageClass = storageClass;
    }

    @XmlElement(name = "Part")
    public List<MultipartPart> getParts() {
        return parts;
    }

    public void setParts(List<MultipartPart> parts) {
        this.parts = parts;
    }
}
