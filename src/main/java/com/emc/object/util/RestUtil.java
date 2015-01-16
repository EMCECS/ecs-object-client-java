package com.emc.object.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public final class RestUtil {
    public static final String HEADER_CONTENT_MD5 = "Content-MD5";

    public static final String X_EMC_PREFIX = "x-emc-";

    private static final String HEADER_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";
    private static final ThreadLocal<DateFormat> headerFormat = new ThreadLocal<DateFormat>();

    public static Map<String, String> getQueryParameterMap(String queryString) {
        Map<String, String> parameters = new HashMap<>();
        if (queryString != null && queryString.trim().length() > 0) {
            for (String pair : queryString.split(",")) {
                String[] keyValue = pair.split("=");
                if (keyValue.length < 1 || keyValue.length > 2 || keyValue[0].trim().length() == 0)
                    throw new IllegalArgumentException("invalid query parameter: " + pair);
                parameters.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
            }
        }
        return parameters;
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

    public static synchronized String headerFormat(Date date) {
        return getHeaderFormat().format(date);
    }

    public static String encodeUtf8(String value) {
        // Use %20, not +
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding isn't supported on this system", e); // unrecoverable
        }
    }

    public static String decodeUtf8(String value) {
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
