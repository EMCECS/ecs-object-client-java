package com.emc.object.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class RestUtil {
    public static final String HEADER_DATE = "Date";
    public static final String HEADER_CONTENT_MD5 = "Content-MD5";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_RANGE = "Range";
    public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String HEADER_IF_UNMODIFIED_SINE = "If-Unmodified-Since";
    public static final String HEADER_IF_MATCH = "If-Match";
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    public static final String HEADER_LAST_MODIFIED = "Last-Modified";
    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_EXPIRES = "Expires";

    public static final String EMC_PREFIX = "x-emc-";

    public static final String EMC_NAMESPACE = EMC_PREFIX + "namespace";
    public static final String EMC_PROJECT_ID = EMC_PREFIX + "project-id";
    public static final String EMC_VPOOL_ID = EMC_PREFIX + "vpool";
    public static final String EMC_FS_ENABLED = EMC_PREFIX + "file-system-access-enabled";

    public static final String TYPE_APPLICATION_XML = "application/xml";
    public static final String TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";

    public static final String PROPERTY_NAMESPACE = "com.emc.object.namespace";

    public static final int STATUS_REDIRECT = 301;
    public static final int STATUS_UNAUTHORIZED = 403;
    public static final int STATUS_NOT_FOUND = 404;

    public static final String DEFAULT_CONTENT_TYPE = TYPE_APPLICATION_OCTET_STREAM;

    private static final String HEADER_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";
    private static final ThreadLocal<DateFormat> headerFormat = new ThreadLocal<DateFormat>();

    public static Object getFirst(Map<String, List<Object>> multiValueMap, String key) {
        List<Object> values = multiValueMap.get(key);
        if (values == null || values.isEmpty()) return null;
        return values.get(0);
    }

    public static void putSingle(Map<String, List<Object>> multiValueMap, String key, Object value) {
        put(multiValueMap, key, value, true);
    }

    public static void add(Map<String, List<Object>> multiValueMap, String key, Object value) {
        put(multiValueMap, key, value, false);
    }

    private static void put(Map<String, List<Object>> multiValueMap, String key, Object value, boolean single) {
        synchronized (multiValueMap) {
            List<Object> values = multiValueMap.get(key);
            if (values == null) {
                values = new ArrayList<>();
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
        Map<String, String> parameters = new HashMap<>();
        if (queryString != null && queryString.trim().length() > 0) {
            for (String pair : queryString.split(",")) {
                String[] keyValue = pair.split("=");
                if (keyValue.length < 1 || keyValue.length > 2 || keyValue[0].trim().length() == 0)
                    throw new IllegalArgumentException("invalid query parameter: " + pair);
                parameters.put(urlDecode(keyValue[0]), keyValue.length > 1 ? urlDecode(keyValue[1]) : "");
            }
        }
        return parameters;
    }

    /**
     * URL-encodes names and values
     */
    public static String generateQueryString(Map<String, Object> parameterMap) {
        StringBuilder query = new StringBuilder();
        if (parameterMap != null && !parameterMap.isEmpty()) {
            Iterator<String> paramI = parameterMap.keySet().iterator();
            while (paramI.hasNext()) {
                String name = paramI.next();
                query.append(urlEncode(name));
                if (parameterMap.get(name) != null)
                    query.append("=").append(urlEncode(parameterMap.get(name).toString()));
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
        return getHeaderFormat().format(date);
    }

    public static Date headerParse(String dateString) {
        try {
            return getHeaderFormat().parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("invalid date header: " + dateString, e);
        }
    }

    public static String urlEncode(String value) {
        // Use %20, not +
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding isn't supported on this system", e); // unrecoverable
        }
    }

    public static String urlDecode(String value) {
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
