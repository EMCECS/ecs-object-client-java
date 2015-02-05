/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3;

public class S3Exception extends RuntimeException {
    private int httpCode;
    private String errorCode;
    private String requestId;

    public S3Exception(String message, int httpCode) {
        super(message);
        this.httpCode = httpCode;
    }

    public S3Exception(String message, int httpCode, Throwable cause) {
        super(message, cause);
        this.httpCode = httpCode;
    }

    public S3Exception(String message, int httpCode, String errorCode, String requestId) {
        super(message);
        this.httpCode = httpCode;
        this.errorCode = errorCode;
        this.requestId = requestId;
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
}
