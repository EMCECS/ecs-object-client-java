/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.util;

import com.emc.object.s3.S3Constants;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
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
    public static final String HEADER_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    public static final String HEADER_LAST_MODIFIED = "Last-Modified";
    public static final String HEADER_RANGE = "Range";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_HOST = "Host";

    public static final String EMC_PREFIX = "x-emc-";

    public static final String EMC_APPEND_OFFSET = EMC_PREFIX + "append-offset";
    public static final String EMC_CONTENT_MD5 = EMC_PREFIX + "content-md5";
    public static final String EMC_FS_ENABLED = EMC_PREFIX + "file-system-access-enabled";
    public static final String EMC_MTIME = EMC_PREFIX + "mtime";
    public static final String EMC_NAMESPACE = EMC_PREFIX + "namespace";
    public static final String EMC_VPOOL = EMC_PREFIX + "dataservice-vpool";
    public static final String EMC_STALE_READ_ALLOWED = EMC_PREFIX + "is-stale-allowed";
    public static final String EMC_ENCRYPTION_ENABLED = EMC_PREFIX + "server-side-encryption-enabled";
    public static final String EMC_RETENTION_PERIOD = EMC_PREFIX + "retention-period";
    public static final String EMC_RETENTION_POLICY = EMC_PREFIX + "retention-policy";
    public static final String EMC_METADATA_SEARCH = EMC_PREFIX + "metadata-search";

    public static final String TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String TYPE_APPLICATION_XML = "application/xml";
    public static final String TYPE_APPLICATION_JSON = "application/json";

    public static final String PROPERTY_NAMESPACE = "com.emc.object.namespace";
    public static final String PROPERTY_USER_METADATA = "com.emc.object.userMetadata";
    public static final String PROPERTY_ENCODE_ENTITY = "com.emc.object.codec.encodeEntity";
    public static final String PROPERTY_DECODE_ENTITY = "com.emc.object.codec.decodeEntity";
    public static final String PROPERTY_KEEP_ENCODE_HEADERS = "com.emc.object.codec.keepEncodeHeaders";
    public static final String PROPERTY_VERIFY_READ_CHECKSUM = "com.emc.object.verifyReadChecksum";
    public static final String PROPERTY_VERIFY_WRITE_CHECKSUM = "com.emc.object.verifyWriteChecksum";
    public static final String PROPERTY_GENERATE_CONTENT_MD5 = "com.emc.object.generateContentMd5";

    public static final int STATUS_REDIRECT = 301;
    public static final int STATUS_UNAUTHORIZED = 403;
    public static final int STATUS_NOT_FOUND = 404;

    public static final String DEFAULT_CONTENT_TYPE = TYPE_APPLICATION_OCTET_STREAM;

    public static final DateTimeFormatter iso8601MillisecondFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(TimeZone.getTimeZone("UTC").toZoneId());

    private static final String HEADER_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String AMZ_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    private static final ThreadLocal<DateFormat> headerFormat = new ThreadLocal<>();
    private static final ThreadLocal<CharsetEncoder> utf8Encoder = ThreadLocal.withInitial(StandardCharsets.UTF_8::newEncoder);

    public static <T> String getFirstAsString(Map<String, List<T>> multiValueMap, String key) {
        return getFirstAsString(multiValueMap, key, false);
    }

    public static <T> String getFirstAsString(Map<String, List<T>> multiValueMap, String key, boolean stripQuotes) {
        List<T> values = multiValueMap.get(key);
        if (values == null || values.isEmpty()) return null;
        Object value = values.get(0);
        if (value == null) return null;
        return stripQuotes ? stripQuotes(value.toString()) : value.toString();
    }

    public static String stripQuotes(String value) {
        if (value == null) return null;
        int start = 0, end = value.length();
        if (value.charAt(0) == '"') start = 1;
        if (value.charAt(value.length() - 1) == '"') end = value.length() - 1;
        return value.substring(start, end);
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
     * @deprecated (2.0.4) use {@link #generateRawQueryString(Map)} instead
     */
    public static String generateQueryString(Map<String, String> parameterMap) {
        return generateRawQueryString(parameterMap);
    }

    /**
     * URL-encodes names and values
     */
    public static String generateRawQueryString(Map<String, String> parameterMap) {
        StringBuilder query = new StringBuilder();
        if (parameterMap != null && !parameterMap.isEmpty()) {
            Iterator<String> paramI = parameterMap.keySet().iterator();
            while (paramI.hasNext()) {
                String name = paramI.next();
                query.append(urlEncode(name));
                // this may be an issue in v4... amazon wants an empty string if there is no value
                if (parameterMap.get(name) != null) {
                    query.append("=").append(urlEncode(parameterMap.get(name)));
                }
                if (paramI.hasNext()) query.append("&");
            }
        }
        return query.toString();
    }

    public static String getRequestDate(long clockSkew) {
        return headerFormat(new Date(System.currentTimeMillis() + clockSkew));
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

    public static Date headerParse(String dateString, String headerKey) {
        if (dateString == null) return null;
        try {
            if(headerKey.equals(S3Constants.AMZ_DATE)) {
                // convert date
                SimpleDateFormat sdf = new SimpleDateFormat(AMZ_DATE_FORMAT);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(dateString);
                sdf.applyPattern(HEADER_FORMAT);
                return date;
            }
            return getHeaderFormat().parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("invalid date header: " + dateString, e);
        }
    }

    public static String getEncodedPath(URI uri) {

        // this is the only way I've found to get the true encoded path
        String rawUri = uri.toASCIIString();
        String path = rawUri.substring(rawUri.indexOf("/", 9));
        if (path.contains("?")) path = path.substring(0, path.indexOf("?"));
        if (path.contains("#")) path = path.substring(0, path.indexOf("#"));
        return path;
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
        return urlDecode(value, true);
    }

    public static String urlDecode(String value, boolean preservePlus) {
        if (value == null) return null;
        try {
            // don't want '+' decoded to a space
            if (preservePlus) value = value.replace("+", "%2B");
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding isn't supported on this system", e); // unrecoverable
        }
    }

    /**
     * Note the rawQuery and rawFragment must already be encoded.  No URL-encoding will be done for parameters here.
     * This is the only way ampersands (&amp;) can be encoded into a parameter value.
     */
    public static URI buildUri(String scheme, String host, int port, String path, String rawQuery, String rawFragment)
            throws URISyntaxException {
        URI uri = new URI(scheme, null, host, port, path, null, null);

        String uriString = toASCIIString(uri);
        if (rawQuery != null) uriString += "?" + rawQuery;
        if (rawFragment != null) uriString += "#" + rawFragment;

        // workaround for https://bugs.openjdk.java.net/browse/JDK-8037396
        uriString = uriString.replace("[", "%5B").replace("]", "%5D");

        // replace double-slash with /%2f (workaround for apache client)
        if (path != null && path.length() > 2 && path.charAt(0) == '/' && path.charAt(1) == '/') {
            int doubleSlashIndex = uriString.indexOf("//");
            if (scheme != null) doubleSlashIndex = uriString.indexOf("//", doubleSlashIndex + 2);
            uriString = uriString.substring(0, doubleSlashIndex) + "/%2F" + uriString.substring(doubleSlashIndex + 2);
        }

        // Special case to handle "+" characters that URI doesn't handle well.
        uriString = uriString.replace("+", "%2B");

        return new URI(uriString);
    }

    /**
     * Returns the content of this URI as a US-ASCII string.
     *
     * <p><b>Note:</b> this starts our customized version of URI's toASCIIString.  We differ in only one aspect: we do
     * NOT normalize Unicode characters.  This is because certain Unicode characters may have different compositions
     * and normalization may change the UTF-8 sequence represented by a character.  We must maintain the same UTF-8
     * sequence in and out and therefore we cannot normalize the sequences.</p>
     *
     * <p> If this URI does not contain any characters in the <i>other</i>
     * category then an invocation of this method will return the same value as
     * an invocation of the {@link #toString() toString} method.  Otherwise
     * this method works as if by invoking that method and then
     * <a href="#encode">encoding</a> the result.  </p>
     *
     * @return  The string form of this URI, encoded as needed
     *          so that it only contains characters in the US-ASCII
     *          charset
     */
    public static String toASCIIString(URI u) {
        String s = defineString(u);
        return encode(s);
    }

    /**
     * Defines a URI string.  Provided for our special URI encoder.
     * @param u URI to encode
     * @return String for the URI
     */
    private static String defineString(URI u) {

        StringBuilder sb = new StringBuilder();
        if (u.getScheme() != null) {
            sb.append(u.getScheme());
            sb.append(':');
        }
        if (u.isOpaque()) {
            sb.append(u.getRawSchemeSpecificPart());
        } else {
            if (u.getHost() != null) {
                sb.append("//");
                if (u.getUserInfo() != null) {
                    sb.append(u.getUserInfo());
                    sb.append('@');
                }
                boolean needBrackets = ((u.getHost().indexOf(':') >= 0)
                        && !u.getHost().startsWith("[")
                        && !u.getHost().endsWith("]"));
                if (needBrackets) sb.append('[');
                sb.append(u.getHost());
                if (needBrackets) sb.append(']');
                if (u.getPort() != -1) {
                    sb.append(':');
                    sb.append(u.getPort());
                }
            } else if (u.getRawAuthority() != null) {
                sb.append("//");
                sb.append(u.getRawAuthority());
            }
            if (u.getRawPath() != null)
                sb.append(u.getRawPath());
            if (u.getRawQuery() != null) {
                sb.append('?');
                sb.append(u.getRawQuery());
            }
        }
        if (u.getRawFragment() != null) {
            sb.append('#');
            sb.append(u.getRawFragment());
        }
        return sb.toString();
    }

    /**
     * Encodes all characters >= \u0080 into escaped, <strikethrough>normalized</strikethrough> UTF-8 octets,
     * assuming that s is otherwise legal
     */
    private static String encode(String s) {
        int n = s.length();
        if (n == 0)
            return s;

        // First check whether we actually need to encode
        for (int i = 0;;) {
            if (s.charAt(i) >= '\u0080')
                break;
            if (++i >= n)
                return s;
        }

        ByteBuffer bb = null;
        try {
            bb = utf8Encoder.get().encode(CharBuffer.wrap(s));
        } catch (CharacterCodingException x) {
            assert false;
        }

        StringBuffer sb = new StringBuffer();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte)b);
            else
                sb.append((char)b);
        }
        return sb.toString();
    }

    private final static char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendEscape(StringBuffer sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }


    public static URI replaceHost(URI uri, String host) throws URISyntaxException {
        return buildUri(uri.getScheme(), host, uri.getPort(), uri.getPath(), uri.getRawQuery(), uri.getRawFragment());
    }

    public static URI replacePath(URI uri, String path) throws URISyntaxException {
        return buildUri(uri.getScheme(), uri.getHost(), uri.getPort(), path, uri.getRawQuery(), uri.getRawFragment());
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

    public static String join(String separator, Iterable<String> items) {
        if(separator == null) throw new IllegalArgumentException("separator argument is null");
        if(items == null) throw new IllegalArgumentException("items argument is null");
        StringBuilder sb = new StringBuilder();
        for(String item : items) {
            if(sb.length() > 0) sb.append(separator);
            sb.append(item);
        }
        return sb.toString();
    }

    private RestUtil() {
    }
}
