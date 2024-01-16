package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement(name = "EmptyBucketStatus")
public class BucketDeletionStatus {

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
    private String status;

    /**
     * Timestamp when the operation was created
     * @valid Date in ISO 8601 format
     */
    private Date created;

    /**
     * Timestamp of the last time the operation status was updated
     * @valid Date in ISO 8601 format
     */
    private Date lastUpdated;

    /**
     * Number of entries deleted
     * @valid none
     */
    private Long entriesDeleted;

    /**
     * Number of entries unable to delete due to retention
     * @valid none
     */
    private Long failedToDeleteRetention;

    /**
     * Number of entries failed to delete due to permission
     * @valid none
     */
    private Long failedToDeletePermission;

    /**
     * Number of entries failed to delete due to failed dangling cleanup
     * @valid none
     */
    private Long failedToDeleteDangling;

    /**
     * Number of entries failed to delete due to other reasons
     * @valid none
     */
    private Long failedToDeleteOther;

    /**
     * Approximate count of objects in the bucket before deletion
     * @valid none
     */
    private Long approximateObjectCount;

    /**
     * Approximate total size of the bucket before deletion
     * @valid none
     */
    private Double approximateTotalSize;

    /**
     * Unit of the approximate bucket total size
     * @valid none
     */
    private String approximateTotalSizeUnitString;

    /**
     * Optional additional information about the status
     * @valid none
     */
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
    public Date getCreated() {
        return created;
    }

    public BucketDeletionStatus setCreated(Date created) {
        this.created = created;
        return this;
    }

    @XmlElement(name = "LastUpdated")
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public BucketDeletionStatus setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    @XmlElement(name = "EntriesDeleted")
    public Long getEntriesDeleted() {
        return entriesDeleted;
    }

    public BucketDeletionStatus setEntriesDeleted(Long entriesDeleted) {
        this.entriesDeleted = entriesDeleted;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToRetention")
    public Long getFailedToDeleteRetention() {
        return failedToDeleteRetention;
    }

    public BucketDeletionStatus setFailedToDeleteRetention(Long failedToDeleteRetention) {
        this.failedToDeleteRetention = failedToDeleteRetention;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToPermission")
    public Long getFailedToDeletePermission() {
        return failedToDeletePermission;
    }

    public BucketDeletionStatus setFailedToDeletePermission(Long failedToDeletePermission) {
        this.failedToDeletePermission = failedToDeletePermission;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToDangling")
    public Long getFailedToDeleteDangling() {
        return failedToDeleteDangling;
    }

    public BucketDeletionStatus setFailedToDeleteDangling(Long failedToDeleteDangling) {
        this.failedToDeleteDangling = failedToDeleteDangling;
        return this;
    }

    @XmlElement(name = "FailedToDeleteDueToOther")
    public Long getFailedToDeleteOther() {
        return failedToDeleteOther;
    }

    public BucketDeletionStatus setFailedToDeleteOther(Long failedToDeleteOther) {
        this.failedToDeleteOther = failedToDeleteOther;
        return this;
    }

    @XmlElement(name = "ApproxObjectCount")
    public Long getApproximateObjectCount() {
        return approximateObjectCount;
    }

    public BucketDeletionStatus setApproximateObjectCount(Long approximateObjectCount) {
        this.approximateObjectCount = approximateObjectCount;
        return this;
    }

    @XmlElement(name = "ApproxTotalSize")
    public Double getApproximateTotalSize() {
        return approximateTotalSize;
    }

    public BucketDeletionStatus setApproximateTotalSize(Double approximateTotalSize) {
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
