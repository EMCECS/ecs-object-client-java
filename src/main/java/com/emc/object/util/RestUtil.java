package com.emc.object.util;

import java.util.*;

public final class RestUtil {
    public static final String HEADER_CONTENT_MD5 = "Content-MD5";

    public static final String X_EMC_PREFIX = "x-emc-";

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
        return Iso8601Adapter.getFormat().format(new Date(System.currentTimeMillis() + clockSkew));
    }

    private RestUtil() {
    }
}
