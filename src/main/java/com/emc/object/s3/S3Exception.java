/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3;

import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Response;

public class S3Exception extends ResponseProcessingException {
    private String errorCode;
    private String requestId;

    public S3Exception(Response response, String message) {
        super(response, message);
    }

    public S3Exception(Response response, String message, Throwable cause) {
        super(response, message, cause);
    }

    public S3Exception(Response response, String message, String errorCode, String requestId) {
        super(response, message);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }
}
