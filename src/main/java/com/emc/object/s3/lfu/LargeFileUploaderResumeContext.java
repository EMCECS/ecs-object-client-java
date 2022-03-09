package com.emc.object.s3.lfu;

import com.emc.object.s3.bean.MultipartPartETag;

import java.util.Date;
import java.util.Map;

public class LargeFileUploaderResumeContext {
    private String uploadId;
    private Map<Integer, MultipartPartETag> partsToSkip;
    private Date resumeIfInitiatedAfter;
    private boolean verifySkippedParts = true;

    public String getUploadId() {
        return uploadId;
    }

    /**
     * Specifies the specific upload ID to resume.
     * If this is not specified, the latest upload will be resumed, and its uploadId will be set here.
     * Part sizes/count will be verified as a sanity check.
     * Keep <code>verifySkippedParts</code> enabled to ensure skipped parts are verified properly.
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Map<Integer, MultipartPartETag> getPartsToSkip() {
        return partsToSkip;
    }

    /**
     * Specifies a map of existing parts to skip.
     * If this is not specified, the configured uploadId will be listed and its parts will be set here.
     * Part sizes/count will be verified as a sanity check.
     * Keep <code>verifySkippedParts</code> enabled to ensure skipped parts are verified properly.
     */
    public void setPartsToSkip(Map<Integer, MultipartPartETag> partsToSkip) {
        this.partsToSkip = partsToSkip;
    }

    public Date getResumeIfInitiatedAfter() {
        return resumeIfInitiatedAfter;
    }

    /**
     * An existing upload will only be resumed if it was initiated after the specified date.
     * Set to source data's lastModified time to avoid using a stale upload.
     */
    public void setResumeIfInitiatedAfter(Date resumeIfInitiatedAfter) {
        this.resumeIfInitiatedAfter = resumeIfInitiatedAfter;
    }

    public boolean isVerifySkippedParts() {
        return verifySkippedParts;
    }

    /**
     * If existing parts are found, this flag will re-read the source part ranges, to verify the
     * ETags of those parts found in the upload.
     * Default is true
     */
    public void setVerifySkippedParts(boolean verifySkippedParts) {
        this.verifySkippedParts = verifySkippedParts;
    }

    /**
     * Specifies the specific upload ID to resume.
     * If this is not specified, the latest upload will be resumed. Part sizes/count will be verified as a sanity check.
     * Keep <code>verifySkippedParts</code> enabled to ensure skipped parts are verified properly.
     */
    public LargeFileUploaderResumeContext withUploadId(String uploadId) {
        setUploadId(uploadId);
        return this;
    }

    /**
     * Specifies a map of existing parts to skip.
     * If this is not specified, the configured uploadId will be listed and its parts will be set here.
     * Part sizes/count will be verified as a sanity check.
     * Keep <code>verifySkippedParts</code> enabled to ensure skipped parts are verified properly.
     */
    public LargeFileUploaderResumeContext withPartsToSkip(Map<Integer, MultipartPartETag> partsToSkip) {
        setPartsToSkip(partsToSkip);
        return this;
    }

    /**
     * An existing upload will only be resumed if it was initiated after the specified date.
     * Set to source data's lastModified time to avoid using a stale upload.
     */
    public LargeFileUploaderResumeContext withResumeIfInitiatedAfter(Date resumeIfInitiatedAfter) {
        setResumeIfInitiatedAfter(resumeIfInitiatedAfter);
        return this;
    }

    /**
     * If existing parts are found, this flag will re-read the source part ranges, to verify the
     * ETags of those parts found in the upload.
     * Default is true
     */
    public LargeFileUploaderResumeContext withVerifySkippedParts(boolean verifySkippedParts) {
        setVerifySkippedParts(verifySkippedParts);
        return this;
    }
}
