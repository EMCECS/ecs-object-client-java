/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class RestUtil {
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_MD5 = "Content-MD5";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_DATE = "Date";
    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_EXPIRES = "Expires";
    public static final String HEADER_IF_MATCH = "If-Match";
    public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    public static final String HEADER_IF_UNMODIFIED_SINE = "If-Unmodified-Since";
    public static final String HEADER_LAST_MODIFIED = "Last-Modified";
    public static final String HEADER_RANGE = "Range";
    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String EMC_PREFIX = "x-emc-";

    public static final String EMC_APPEND_OFFSET = EMC_PREFIX + "append-offset";
    public static final String EMC_FS_ENABLED = EMC_PREFIX + "file-system-access-enabled";
    public static final String EMC_NAMESPACE = EMC_PREFIX + "namespace";
    public static final String EMC_VPOOL = EMC_PREFIX + "vpool";

    public static final String TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String TYPE_APPLICATION_XML = "application/xml";

    public static final String PROPERTY_NAMESPACE = "com.emc.object.namespace";
    public static final String PROPERTY_USER_METADATA = "com.emc.object.userMetadata";
    public static final String PROPERTY_ENCODE_ENTITY = "com.emc.object.codec.encodeEntity";
    public static final String PROPERTY_DECODE_ENTITY = "com.emc.object.codec.decodeEntity";
    public static final String PROPERTY_VERIFY_READ_CHECKSUM = "com.emc.object.verifyReadChecksum";
    public static final String PROPERTY_VERIFY_WRITE_CHECKSUM = "com.emc.object.verifyWriteChecksum";


    public static final int STATUS_REDIRECT = 301;
    public static final int STATUS_UNAUTHORIZED = 403;
    public static final int STATUS_NOT_FOUND = 404;

    public static final String DEFAULT_CONTENT_TYPE = TYPE_APPLICATION_OCTET_STREAM;

    private static final String HEADER_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";
    private static final ThreadLocal<DateFormat> headerFormat = new ThreadLocal<DateFormat>();

    public static <T> String getFirstAsString(Map<String, List<T>> multiValueMap, String key) {
        List<T> values = multiValueMap.get(key);
        if (values == null || values.isEmpty()) return null;
        Object value = values.get(0);
        return value == null ? null : value.toString();
    }

    public static void putSingle(Map<String, List<Object>> multiValueMap, String key, Object value) {
        put(multiValueMap, key, value, true);
    }

    public static void add(Map<String, List<Object>> multiValueMap, String key, Object value) {
        put(multiValueMap, key, value, false);
    }

    private static void put(Map<String, List<Object>> multiValueMap, String key, Object value, boolean single) {
        synchronized (multiValueMap) {
            // save calling code some headaches
            if (value == null) {
                if (single) multiValueMap.remove(key);
                return;
            }
            List<Object> values = multiValueMap.get(key);
            if (values == null) {
                values = new ArrayList<Object>();
                multiValueMap.put(key, values);
            } else if (single)
                values.clear();
            values.add(value);
        }
    }

    /**
     * URL-decodes names and values
     */
    public static Map<String, String> getQueryParameterMap(String queryString) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (queryString != null && queryString.trim().length() > 0) {
            for (String pair : queryString.split("&")) {
                int equals = pair.indexOf('=');
                if (equals == 0) throw new IllegalArgumentException("invalid query parameter: " + pair);

                String key = equals > 0 ? pair.substring(0, equals) : pair;
                String value = equals > 0 ? pair.substring(equals + 1) : null;

                if (key.trim().length() == 0) throw new IllegalArgumentException("query parameters must have a name");

                parameters.put(urlDecode(key), urlDecode(value));
            }
        }
        return parameters;
    }

    /**
     * URL-encodes names and values
     */
    public static String generateQueryString(Map<String, String> parameterMap) {
        StringBuilder query = new StringBuilder();
        if (parameterMap != null && !parameterMap.isEmpty()) {
            Iterator<String> paramI = parameterMap.keySet().iterator();
            while (paramI.hasNext()) {
                String name = paramI.next();
                query.append(urlEncode(name));
                if (parameterMap.get(name) != null)
                    query.append("=").append(urlEncode(parameterMap.get(name)));
                if (paramI.hasNext()) query.append("&");
            }
        }
        return query.toString();
    }

    public static String getRequestDate(long clockSkew) {
        return headerFormat(new Date(System.currentTimeMillis() + clockSkew));
    }

    public static String delimit(List<Object> values, String delimiter) {
        if (values == null || values.isEmpty()) return null;
        StringBuilder delimited = new StringBuilder();
        Iterator<Object> valuesI = values.iterator();
        while (valuesI.hasNext()) {
            delimited.append(valuesI.next());
            if (valuesI.hasNext()) delimited.append(delimiter);
        }
        return delimited.toString();
    }

    public static String headerFormat(Date date) {
        if (date == null) return null;
        return getHeaderFormat().format(date);
    }

    public static Date headerParse(String dateString) {
        if (dateString == null) return null;
        try {
            return getHeaderFormat().parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("invalid date header: " + dateString, e);
        }
    }

    public static String urlEncode(String value) {
        if (value == null) return null;
        // Use %20, not +
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding isn't supported on this system", e); // unrecoverable
        }
    }

    public static String urlDecode(String value) {
        if (value == null) return null;
        try {
            // don't want '+' decoded to a space
            return URLDecoder.decode(value.replace("+", "%2B"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding isn't supported on this system", e); // unrecoverable
        }
    }

    private static DateFormat getHeaderFormat() {
        DateFormat format = headerFormat.get();
        if (format == null) {
            format = new SimpleDateFormat(HEADER_FORMAT, Locale.ENGLISH);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            headerFormat.set(format);
        }
        return format;
    }

    private RestUtil() {
    }
}
