package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement(name = "EmptyBucketStatus")
public class BucketDeletionStatus {
    
    private String status;
    
    private Date created;
    
    private Date lastUpdated;

    private Long entriesDeleted;

    private Long failedToDeleteRetention;

    private Long failedToDeletePermission;

    private Long failedToDeleteDangling;

    private Long failedToDeleteOther;

    private Long approximateObjectCount;

    private Double approximateTotalSize;

    private String approximateTotalSizeUnitString;

    private String message;

    /**
     *  Overall status of the bucket delete task
     *  Valid values: PENDING, IN_PROGRESS, POST_PROCESSING, DONE, FAILED, ABORT_IN_PROGRESS, ABORTED
     *  <br>
     *  PENDING - The task has been created and will start execution once it is successfully configured.
     *  <br>
     *  IN_PROGRESS - The task is executing normally.
     *  <br>
     *  POST_PROCESSING - The task is finsished processing and is finalizing before moving to the DONE or FAILED state.
     *  <br>
     *  DONE - The task successfully completed and the bucket was deleted.
     *  <br>
     *  ABORT_IN_PROGRESS - The tasks is aborting and is finalizing before moving to the ABORTED state.
     *  <br>
     *  ABORTED - The task was stopped before it could complete and should be retried.
     *  <br>
     *  FAILED - The task completed but not all items associated with the bucket could be removed.
     *  <br>
     *  <br>
     *  When the status is FAILED the failed_to_delete_n counts give an indication of why certain items could not be removed by the task.
     *
     */
    @XmlElement(name="Status")
    public String getStatus() {
        return status;
    }

    /**
     * {@link BucketDeletionStatus#getStatus}
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Timestamp when the operation was created
     * @valid Date in ISO 8601 format
     */
    @XmlElement(name="Created")
    public Date getCreated() {
        return created;
    }

    /**
     * {@link BucketDeletionStatus#getCreated}
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Timestamp of the last time the operation status was updated
     * @valid Date in ISO 8601 format
     */
    @XmlElement(name = "LastUpdated")
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * {@link BucketDeletionStatus#getLastUpdated}
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Number of entries deleted
     * @valid none
     */
    @XmlElement(name = "EntriesDeleted")
    public Long getEntriesDeleted() {
        return entriesDeleted;
    }

    /**
     * {@link BucketDeletionStatus#getEntriesDeleted}
     */
    public void setEntriesDeleted(Long entriesDeleted) {
        this.entriesDeleted = entriesDeleted;
    }

    /**
     * Number of entries unable to delete due to retention
     * @valid none
     */
    @XmlElement(name = "FailedToDeleteDueToRetention")
    public Long getFailedToDeleteRetention() {
        return failedToDeleteRetention;
    }

    /**
     * {@link BucketDeletionStatus#getFailedToDeleteRetention}
     */
    public void setFailedToDeleteRetention(Long failedToDeleteRetention) {
        this.failedToDeleteRetention = failedToDeleteRetention;
    }

    /**
     * Number of entries failed to delete due to permission
     * @valid none
     */
    @XmlElement(name = "FailedToDeleteDueToPermission")
    public Long getFailedToDeletePermission() {
        return failedToDeletePermission;
    }

    /**
     * {@link BucketDeletionStatus#getFailedToDeletePermission}
     */
    public void setFailedToDeletePermission(Long failedToDeletePermission) {
        this.failedToDeletePermission = failedToDeletePermission;
    }

    /**
     * Number of entries failed to delete due to failed dangling cleanup
     * @valid none
     */
    @XmlElement(name = "FailedToDeleteDueToDangling")
    public Long getFailedToDeleteDangling() {
        return failedToDeleteDangling;
    }

    /**
     * {@link BucketDeletionStatus#getFailedToDeleteDangling}
     */
    public void setFailedToDeleteDangling(Long failedToDeleteDangling) {
        this.failedToDeleteDangling = failedToDeleteDangling;
    }

    /**
     * Number of entries failed to delete due to other reasons
     * @valid none
     */
    @XmlElement(name = "FailedToDeleteDueToOther")
    public Long getFailedToDeleteOther() {
        return failedToDeleteOther;
    }

    /**
     * {@link BucketDeletionStatus#getFailedToDeleteOther}
     */
    public void setFailedToDeleteOther(Long failedToDeleteOther) {
        this.failedToDeleteOther = failedToDeleteOther;
    }

    /**
     * Approximate count of objects in the bucket before deletion
     * @valid none
     */
    @XmlElement(name = "")
    public Long getApproximateObjectCount() {
        return approximateObjectCount;
    }

    /**
     * {@link BucketDeletionStatus#getApproximateObjectCount}
     */
    public void setApproximateObjectCount(Long approximateObjectCount) {
        this.approximateObjectCount = approximateObjectCount;
    }

    /**
     * Approximate total size of the bucket before deletion
     * @valid none
     */
    @XmlElement(name = "ApproximateTotalSize")
    public Double getApproximateTotalSize() {
        return approximateTotalSize;
    }

    /**
     * {@link BucketDeletionStatus#getApproximateTotalSize}
     */
    public void setApproximateTotalSize(Double approximateTotalSize) {
        this.approximateTotalSize = approximateTotalSize;
    }

    /**
     * Unit of the approximate bucket total size
     * @valid none
     */
    @XmlElement(name = "ApproximateTotalSizeUnitString")
    public String getApproximateTotalSizeUnitString() {
        return approximateTotalSizeUnitString;
    }

    /**
     * {@link BucketDeletionStatus#getApproximateTotalSizeUnitString}
     */
    public void setApproximateTotalSizeUnitString(String approximateTotalSizeUnitString) {
        this.approximateTotalSizeUnitString = approximateTotalSizeUnitString;
    }

    /**
     * Optional additional information about the status
     * @valid none
     */
    @XmlElement(name = "Message")
    public String getMessage() {
        return message;
    }

    /**
     * {@link BucketDeletionStatus#getMessage}
     */
    public void setMessage(String message) {
        this.message = message;
    }

    public BucketDeletionStatus withStatus(String status) {
        setStatus(status);
        return this;
    }

    public BucketDeletionStatus withCreated(Date created) {
        setCreated(created);
        return this;
    }

    public BucketDeletionStatus withLastUpdated(Date lastUpdated) {
        setLastUpdated(lastUpdated);
        return this;
    }

    public BucketDeletionStatus withEntriesDeleted(Long entriesDeleted) {
        setEntriesDeleted(entriesDeleted);
        return this;
    }

    public BucketDeletionStatus withFailedToDeleteRetention(Long failedToDeleteRetention) {
        setFailedToDeleteRetention(failedToDeleteRetention);
        return this;
    }

    public BucketDeletionStatus withFailedToDeletePermission(Long failedToDeletePermission) {
        setFailedToDeletePermission(failedToDeletePermission);
        return this;
    }

    public BucketDeletionStatus withFailedToDeleteDangling(Long failedToDeleteDangling) {
        setFailedToDeleteDangling(failedToDeleteDangling);
        return this;
    }

    public BucketDeletionStatus withFailedToDeleteOther(Long failedToDeleteOther) {
        setFailedToDeleteOther(failedToDeleteOther);
        return this;
    }

    public BucketDeletionStatus withApproximateObjectCount(Long approximateObjectCount) {
        setApproximateObjectCount(approximateObjectCount);
        return this;
    }

    public BucketDeletionStatus withApproximateTotalSize(Double approximateTotalSize) {
        setApproximateTotalSize(approximateTotalSize);
        return this;
    }

    public BucketDeletionStatus withApproximateTotalSizeUnitString(String approximateTotalSizeUnitString) {
        setApproximateTotalSizeUnitString(approximateTotalSizeUnitString);
        return this;
    }

    public BucketDeletionStatus withMessage(String message) {
        setMessage(message);
        return this;
    }

}
