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
    public BucketDeletionStatus setStatus(String status) {
        this.status = status;
        return this;
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
    public BucketDeletionStatus setCreated(Date created) {
        this.created = created;
        return this;
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
    public BucketDeletionStatus setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
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
    public BucketDeletionStatus setEntriesDeleted(Long entriesDeleted) {
        this.entriesDeleted = entriesDeleted;
        return this;
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
    public BucketDeletionStatus setFailedToDeleteRetention(Long failedToDeleteRetention) {
        this.failedToDeleteRetention = failedToDeleteRetention;
        return this;
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
    public BucketDeletionStatus setFailedToDeletePermission(Long failedToDeletePermission) {
        this.failedToDeletePermission = failedToDeletePermission;
        return this;
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
    public BucketDeletionStatus setFailedToDeleteDangling(Long failedToDeleteDangling) {
        this.failedToDeleteDangling = failedToDeleteDangling;
        return this;
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
    public BucketDeletionStatus setFailedToDeleteOther(Long failedToDeleteOther) {
        this.failedToDeleteOther = failedToDeleteOther;
        return this;
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
    public BucketDeletionStatus setApproximateObjectCount(Long approximateObjectCount) {
        this.approximateObjectCount = approximateObjectCount;
        return this;
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
    public BucketDeletionStatus setApproximateTotalSize(Double approximateTotalSize) {
        this.approximateTotalSize = approximateTotalSize;
        return this;
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
    public BucketDeletionStatus setApproximateTotalSizeUnitString(String approximateTotalSizeUnitString) {
        this.approximateTotalSizeUnitString = approximateTotalSizeUnitString;
        return this;
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
    public BucketDeletionStatus setMessage(String message) {
        this.message = message;
        return this;
    }

}
