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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface LargeFileUpload {
    /**
     * Blocks until the upload is complete.
     */
    void waitForCompletion();

    /**
     * Waits if necessary for at most the given time for the upload to complete.
     *
     * @param timeout     the maximum time to wait
     * @param timeoutUnit the time unit of the timeout argument
     * @throws TimeoutException if the wait timed out
     */
    void waitForCompletion(long timeout, TimeUnit timeoutUnit) throws TimeoutException;

    /**
     * Pauses this upload and returns a ResumeContext that can be used to later resume it from the same source data.
     * This method first prevents any part uploads that have not yet started transferring from starting.
     * Then it allows any parts that are currently in-transfer to complete, after which it will return a resume context
     * that contains the upload ID and a list of fully uploaded parts.
     */
    LargeFileUploaderResumeContext pause();

    /**
     * Stops upload activity for this upload and aborts the MPU in the bucket.
     * This method returns immediately.
     */
    void abort();
}
