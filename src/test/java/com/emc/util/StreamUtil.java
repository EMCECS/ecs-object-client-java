package com.emc.util;

import java.io.IOException;
import java.io.InputStream;

public final class StreamUtil {
    public static String readAsString(InputStream inputStream) throws IOException {
        try (InputStream autoClose = inputStream) {
            return new java.util.Scanner(autoClose, "UTF-8").useDelimiter("\\A").next();
        }
    }

    private StreamUtil() {
    }
}
