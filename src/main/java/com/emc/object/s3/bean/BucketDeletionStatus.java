package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "EmptyBucketStatus")
public class BucketDeletionStatus {
    private String status;

    private String created;

    private String lastUpdated;

    private long deleted;

    private long failedToDeleteRetention;

    private long failedToDeletePermission;

    private long failedToDeleteDangling;

    private long failedToDeleteOther;

    private long approximateObjectCount;

    private double approximateTotalSize;

    private String approximateTotalSizeUnitString;

    private String message;

    @XmlElement(name="Status")
    public String getStatus() {
        return status;
    }

    public BucketDeletionStatus setStatus(String status) {
        this.status = status;
        return this;
    }

    @XmlElement(name="Created")
    public String getCreated() {
        return created;
    }

    public BucketDeletionStatus setCreated(String created) {
        this.created = created;
        return this;
    }

    @XmlElement(name = "LastUpdated")
    public String getLastUpdated() {
        return lastUpdated;
    }

    public BucketDeletionStatus setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    @XmlElement(name = "EntriesDeleted")
    public long getDeleted() {
        return deleted;
    }

    public BucketDeletionStatus setDeleted(long deleted) {
        this.deleted = deleted;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToRetention")
    public long getFailedToDeleteRetention() {
        return failedToDeleteRetention;
    }

    public BucketDeletionStatus setFailedToDeleteRetention(long failedToDeleteRetention) {
        this.failedToDeleteRetention = failedToDeleteRetention;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToPermission")
    public long getFailedToDeletePermission() {
        return failedToDeletePermission;
    }

    public BucketDeletionStatus setFailedToDeletePermission(long failedToDeletePermission) {
        this.failedToDeletePermission = failedToDeletePermission;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToDangling")
    public long getFailedToDeleteDangling() {
        return failedToDeleteDangling;
    }

    public BucketDeletionStatus setFailedToDeleteDangling(long failedToDeleteDangling) {
        this.failedToDeleteDangling = failedToDeleteDangling;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToOther")
    public long getFailedToDeleteOther() {
        return failedToDeleteOther;
    }

    public BucketDeletionStatus setFailedToDeleteOther(long failedToDeleteOther) {
        this.failedToDeleteOther = failedToDeleteOther;
        return this;
    }

    @XmlElement(name = "ApproxObjectCount")
    public long getApproximateObjectCount() {
        return approximateObjectCount;
    }

    public BucketDeletionStatus setApproximateObjectCount(long approximateObjectCount) {
        this.approximateObjectCount = approximateObjectCount;
        return this;
    }

    @XmlElement(name = "ApproxTotalSize")
    public double getApproximateTotalSize() {
        return approximateTotalSize;
    }

    public BucketDeletionStatus setApproximateTotalSize(double approximateTotalSize) {
        this.approximateTotalSize = approximateTotalSize;
        return this;
    }

    @XmlElement(name = "ApproxTotalSizeUnitString")
    public String getApproximateTotalSizeUnitString() {
        return approximateTotalSizeUnitString;
    }

    public BucketDeletionStatus setApproximateTotalSizeUnitString(String approximateTotalSizeUnitString) {
        this.approximateTotalSizeUnitString = approximateTotalSizeUnitString;
        return this;
    }

    @XmlElement(name = "Message")
    public String getMessage() {
        return message;
    }

    public BucketDeletionStatus setMessage(String message) {
        this.message = message;
        return this;
    }

}
