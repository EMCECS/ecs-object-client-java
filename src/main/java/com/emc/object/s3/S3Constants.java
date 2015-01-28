package com.emc.object.s3;

public final class S3Constants {
    public static final String XML_NAMESPACE = "http://s3.amazonaws.com/doc/2006-03-01";

    public static final String AMZ_PREFIX = "x-amz-";
    public static final String AMZ_META_PREFIX = AMZ_PREFIX + "meta-";

    public static final String AMZ_DATE = AMZ_PREFIX + "date";
    public static final String AMZ_ACL = AMZ_PREFIX + "acl";
    public static final String AMZ_GRANT_READ = AMZ_PREFIX + "grant-read";
    public static final String AMZ_GRANT_WRITE = AMZ_PREFIX + "grant-write";
    public static final String AMZ_GRANT_READ_ACP = AMZ_PREFIX + "grant-read-acp";
    public static final String AMZ_GRANT_WRITE_ACP = AMZ_PREFIX + "grant-write-acp";
    public static final String AMZ_GRANT_FULL_CONTROL = AMZ_PREFIX + "grant-full-control";
    public static final String AMZ_VERSION_ID = AMZ_PREFIX + "version-id";
    public static final String AMZ_EXPIRATION = AMZ_PREFIX + "expiration";

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
    public static final String PARAM_LIMIT = "limit";
    public static final String PARAM_MARKER = "marker";
    public static final String PARAM_PREFIX = "prefix";
    public static final String PARAM_DELIMITER = "delimiter";
    public static final String PARAM_MAX_KEYS = "max-keys";
    public static final String PARAM_ENCODING_TYPE = "encoding-type";
    public static final String PARAM_KEY_MARKER = "key-marker";
    public static final String PARAM_VERSION_ID_MARKER = "version-id-marker";
    public static final String PARAM_MAX_UPLOADS = "max-uploads";
    public static final String PARAM_UPLOAD_ID_MARKER = "upload-id-marker";
    public static final String PARAM_UPLOAD_ID = "uploadId";
    public static final String PARAM_PART_NUMBER = "partNumber";

    public static final String PROPERTY_BUCKET_NAME = "com.emc.object.s3.bucketName";

    private S3Constants() {
    }
}
