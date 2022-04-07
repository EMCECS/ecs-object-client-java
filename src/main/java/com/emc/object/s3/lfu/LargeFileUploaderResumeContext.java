/*
 * Copyright 2015-2022 Dell Technologies
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of EMC Corporation may not be used to endorse or promote
 *    products derived from this software without specific prior written
 *    permission.
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
    private boolean overwriteMismatchedParts = true;

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

    public boolean isOverwriteMismatchedParts() {
        return overwriteMismatchedParts;
    }

    /**
     * If <code>verifyPartsFoundInTarget</code> is true, and during verification of an existing part, the ETag does not
     * match, that part will be re-uploaded.
     * Default is true
     */
    public void setOverwriteMismatchedParts(boolean overwriteMismatchedParts) {
        this.overwriteMismatchedParts = overwriteMismatchedParts;
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

    /**
     * @see #setOverwriteMismatchedParts(boolean)
     */
    public LargeFileUploaderResumeContext withOverwriteMismatchedParts(boolean overwriteMismatchedParts) {
        setOverwriteMismatchedParts(overwriteMismatchedParts);
        return this;
    }
}
