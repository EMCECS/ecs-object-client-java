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
package com.emc.object.s3;

import com.emc.rest.smart.SmartClientException;

public class S3Exception extends SmartClientException {
    private final int httpCode;
    private String errorCode;
    private String requestId;

    public S3Exception(String message, int httpCode) {
        super(message);
        this.httpCode = httpCode;
        this.setErrorType(fromHttpCode(httpCode));
    }

    public S3Exception(String message, int httpCode, Throwable cause) {
        super(message, cause);
        this.httpCode = httpCode;
        this.setErrorType(fromHttpCode(httpCode));
    }

    public S3Exception(String message, int httpCode, String errorCode, String requestId) {
        super(message);
        this.httpCode = httpCode;
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.setErrorType(fromHttpCode(httpCode));
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }

    private ErrorType fromHttpCode(int httpCode) {
        return httpCode >= 400 && httpCode < 500 ? ErrorType.Client
                : httpCode >= 500 && httpCode < 600 ? ErrorType.Service
                : ErrorType.Unknown;
    }
}
