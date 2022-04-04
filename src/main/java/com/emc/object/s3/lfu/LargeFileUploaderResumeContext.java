package com.emc.object.s3.lfu;

import com.emc.object.s3.bean.MultipartPartETag;

import java.util.Map;

/**
 * Used to resume an existing incomplete MPU.
 * <p>
 * You *must* provide an <code>uploadId</code> to resume.
 * All parts provided in <code>uploadedParts</code> are trusted and assumed to be already verified.
 * If you do not provide a map of <code>uploadedParts</code>, the MPU uploadId parts will be listed. In this case,
 * the listed parts will *not* be trusted. All parts in the list will be sanity checked, and re-read from the source
 * data to create an accurate part ETag manifest and verify all object data as per S3 best practices. If you do not
 * wish to re-verify existing parts found in the target, you can set <code>verifyPartsFoundInTarget</code> to false
 * (not recommended).
 */
public class LargeFileUploaderResumeContext {
    private String uploadId;
    private Map<Integer, MultipartPartETag> uploadedParts = null;
    private boolean verifyPartsFoundInTarget = true;

    public String getUploadId() {
        return uploadId;
    }

    /**
     * Specifies the specific upload ID to resume.
     * This is required to resume an MPU.
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Map<Integer, MultipartPartETag> getUploadedParts() {
        return uploadedParts;
    }

    /**
     * Specifies a map of existing parts to skip.
     * If a part list is provided here, it is trusted, and used in the final part ETag manifest.
     * If this is not specified, the configured uploadId will be listed to identify parts to skip. However, that list
     * will *not* be trusted, so all part sizes/count will be verified as a sanity check, and the part ranges will be
     * re-read from the source to create an accurate part ETag manifest and verify all object data.
     */
    public void setUploadedParts(Map<Integer, MultipartPartETag> uploadedParts) {
        this.uploadedParts = uploadedParts;
    }

    public boolean isVerifyPartsFoundInTarget() {
        return verifyPartsFoundInTarget;
    }

    /**
     * If <code>uploadedParts</code> is *not* provided, and existing parts are found in the target, this flag will
     * re-read the source part ranges, to create a valid part ETag manifest and verify the entire dataset of the object.
     * Default is true
     */
    public void setVerifyPartsFoundInTarget(boolean verifyPartsFoundInTarget) {
        this.verifyPartsFoundInTarget = verifyPartsFoundInTarget;
    }

    /**
     * @see #setUploadId(String)
     */
    public LargeFileUploaderResumeContext withUploadId(String uploadId) {
        setUploadId(uploadId);
        return this;
    }

    /**
     * @see #setUploadedParts(Map)
     */
    public LargeFileUploaderResumeContext withUploadedParts(Map<Integer, MultipartPartETag> uploadedParts) {
        setUploadedParts(uploadedParts);
        return this;
    }

    /**
     * @see #setVerifyPartsFoundInTarget(boolean)
     */
    public LargeFileUploaderResumeContext withVerifyPartsFoundInTarget(boolean verifyPartsFoundInTarget) {
        setVerifyPartsFoundInTarget(verifyPartsFoundInTarget);
        return this;
    }
}
