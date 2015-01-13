package com.emc.object.s3;

public final class S3Constants {
    public static final String XML_NAMESPACE = "http://s3.amazonaws.com/doc/2006-03-01";

    public static final String X_AMZ_PREFIX = "x-amz-";

    public static final String HEADER_DATE = "x-amz-date";

    public static final String HEADER_NAMESPACE = "x-emc-namespace";

    public static final String PARAM_EXPIRES = "Expires";
    public static final String PARAM_RESPONSE_HEADER_CACHE_CONTROL = "response-cache-control";
    public static final String PARAM_RESPONSE_HEADER_CONTENT_DISPOSITION = "response-content-disposition";
    public static final String PARAM_RESPONSE_HEADER_CONTENT_ENCODING = "response-content-encoding";
    public static final String PARAM_RESPONSE_HEADER_CONTENT_LANGUAGE = "response-content-language";
    public static final String PARAM_RESPONSE_HEADER_CONTENT_TYPE = "response-content-type";
    public static final String PARAM_RESPONSE_HEADER_EXPIRES = "response-expires";
    public static final String PARAM_ACCESS_MODE = "accessmode";
    public static final String PARAM_FILE_ACCESS = "fileaccess";
    public static final String PARAM_ENDPOINT = "endpoint";

    private S3Constants() {
    }
}
