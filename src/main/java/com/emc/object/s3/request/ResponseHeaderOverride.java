/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

public enum ResponseHeaderOverride {
    CONTENT_TYPE("response-content-type"),
    CONTENT_LANGUAGE("response-content-language"),
    EXPIRES("response-expires"),
    CACHE_CONTROL("response-cache-control"),
    CONTENT_DISPOSITION("response-content-disposition"),
    CONTENT_ENCODING("response-content-encoding");

    private String queryParam;

    private ResponseHeaderOverride(String queryParam) {
        this.queryParam = queryParam;
    }

    public String getQueryParam() {
        return queryParam;
    }
}
