package com.emc.object.s3.bean;

import com.emc.object.s3.request.EncodingType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
    private Boolean truncated;
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

    @XmlElement(name = "Truncated")
    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
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
