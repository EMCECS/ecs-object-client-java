package com.emc.object.s3;

import javax.ws.rs.WebApplicationException;

public class S3Exception extends WebApplicationException {
    private String errorCode;
    private String requestId;

    public S3Exception(String message) {
        super(message);
    }

    public S3Exception(String message, int httpStatus) {
        super(message, httpStatus);
    }

    public S3Exception(String message, int httpStatus, String errorCode, String requestId) {
        super(message, httpStatus);
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
