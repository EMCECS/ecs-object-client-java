package com.emc.object.s3;

import com.emc.object.s3.jersey.BucketFilter;
import com.emc.object.s3.jersey.NamespaceFilter;
import com.emc.object.s3.request.PresignedUrlRequest;
import com.emc.object.s3.request.ResponseHeaderOverride;
import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.ClientRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class S3SignerV4 extends S3Signer {
    private static final String HEADER_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    //The timestamp must be in UTC and in the following ISO 8601 format: YYYYMMDD'T'HHMMSS'Z'
    private static final String AMZ_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    private static final String AMZ_DATE_FORMAT_SHORT = "yyyyMMdd";
    private static final long PRESIGN_URL_MAX_EXPIRATION_SECONDS = 60 * 60 * 24 * 7;
    private static final SortedSet<String> excludedSignedHeaders = new TreeSet(Arrays.asList(
        "authorization"
    ));

    public S3SignerV4(S3Config s3Config) {
        super(s3Config);
    }

    public void sign(ClientRequest request, String resource, Map<String, String> parameters, Map<String, List<Object>> headers) {
        // # Preparation, add x-amz-date and host headers
        String date = null;
        String serviceType = getServiceType();
        if (headers.containsKey(S3Constants.AMZ_DATE)) {
            date = RestUtil.getFirstAsString(headers, S3Constants.AMZ_DATE);
        }
        else {
            date = getDate(parameters, headers);
        }
        String shortDate = getShortDate(date);
        addHeadersForV4(request.getURI(), date, headers);

        // #1 Create a canonical request for Signature Version 4
        String canonicalRequest = getCanonicalRequest(request, parameters, headers);

        // #2 Create a string to sign for Signature Version 4
        String stringToSign = getStringToSign(request.getMethod(), resource, parameters, headers, date, serviceType, canonicalRequest);
        log.debug("StringToSign: {}", stringToSign);

        // Get signed headers
        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);
        StringBuilder signedHeaders = new StringBuilder();
        for (String name : canonicalizedHeaders.keySet()) {
            if (signedHeaders.length() != 0)
                signedHeaders.append(";");
            signedHeaders.append(name);
        }

        // #3 Calculate the signature for AWS Signature Version 4
        byte[] key = getSigningKey(shortDate, serviceType);
        String signature = getSignature(stringToSign, key);
        log.debug("Signature: {}", signature);

        // #4 Adding signing information to the authorization header
        RestUtil.putSingle(headers, "Authorization", S3Constants.AWS_HMAC_SHA256_ALGORITHM +
                " Credential=" + s3Config.getIdentity() + "/" + shortDate +
                "/" + S3Constants.AWS_DEFAULT_REGION + "/" + serviceType + "/" + S3Constants.AWS_V4_TERMINATOR +
                ", SignedHeaders=" + signedHeaders.toString() + ", " + S3Constants.PARAM_SIGNATURE + "= " + signature);
    }

    protected void addHeadersForV4(URI uri, String date, Map<String, List<Object>> headers) {
        StringBuilder hostHeader = new StringBuilder(uri.getHost());
        // If default port is used, do not include the port in host header
        if (!(s3Config.getProtocol().equals("https") && uri.getPort() == 443) &&
                !(s3Config.getProtocol().equals("http") && uri.getPort() == 80))
            hostHeader.append(":").append(uri.getPort());
        if (!headers.containsKey(S3Constants.AMZ_DATE)) {
            RestUtil.putSingle(headers, S3Constants.AMZ_DATE, date);
        }
        RestUtil.putSingle(headers, RestUtil.HEADER_HOST, hostHeader);
    }

    // This is to generate canonical request for presigned URLs
    // Payload is UNSIGNED-PAYLOAD
    protected String getCanonicalRequest(String method, URI uri, Map<String, String> parameters, Map<String, List<Object>> headers) {
        /*
        CanonicalRequest =
            HTTPRequestMethod + '\n' +
            CanonicalURI + '\n' +
            CanonicalQueryString + '\n' +
            CanonicalHeaders + '\n' +
            SignedHeaders + '\n' +
            UNSIGNED-PAYLOAD
         */
        StringBuilder canonicalRequest = new StringBuilder();
        canonicalRequest.append(method).append("\n");
        String resource = RestUtil.getEncodedPath(uri);
        canonicalRequest.append(resource).append("\n");
        canonicalRequest.append(getCanonicalizedQueryString(parameters));

        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);
        StringBuilder signedHeaders = new StringBuilder();
        for (String name : canonicalizedHeaders.keySet()) {
            canonicalRequest.append(name).append(":").append(canonicalizedHeaders.get(name).trim());
            canonicalRequest.append("\n");
            if (signedHeaders.length() != 0)
                signedHeaders.append(";");
            signedHeaders.append(name);
        }
        canonicalRequest.append("\n");

        signedHeaders.append("\n");
        canonicalRequest.append(signedHeaders);

        canonicalRequest.append(S3Constants.AMZ_UNSIGNED_PAYLOAD);
        log.debug("CanonicalRequest: {}", canonicalRequest);
        return canonicalRequest.toString();
    }

    protected String getCanonicalRequest(ClientRequest request, Map<String, String> parameters, Map<String, List<Object>> headers) {
        /*
        CanonicalRequest =
            HTTPRequestMethod + '\n' +
            CanonicalURI + '\n' +
            CanonicalQueryString + '\n' +
            CanonicalHeaders + '\n' +
            SignedHeaders + '\n' +
            HexEncode(Hash(RequestPayload))
         */
        StringBuilder canonicalRequest = new StringBuilder();
        canonicalRequest.append(request.getMethod()).append("\n");
        URI uri = request.getURI();
        String resource = RestUtil.getEncodedPath(uri);
        canonicalRequest.append(resource).append("\n");
        canonicalRequest.append(getCanonicalizedQueryString(parameters));

        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);
        StringBuilder signedHeaders = new StringBuilder();
        for (String name : canonicalizedHeaders.keySet()) {
            canonicalRequest.append(name).append(":");
            if (canonicalizedHeaders.get(name) != null) {
                canonicalRequest.append(canonicalizedHeaders.get(name).trim());
            }
            canonicalRequest.append("\n");
            if (signedHeaders.length() != 0)
                signedHeaders.append(";");
            signedHeaders.append(name);
        }
        canonicalRequest.append("\n");

        signedHeaders.append("\n");
        canonicalRequest.append(signedHeaders);

        String hashedPayload = "";
        String payload = "";
        byte[] hash = hash256(payload);
        hashedPayload = hexEncode(hash);
        canonicalRequest.append(hashedPayload);
        log.debug("CanonicalRequest: {}" + canonicalRequest);
        return canonicalRequest.toString();
    }

    private String getCanonicalizedQueryString(Map<String, String> parameters) {
        StringBuilder queryString = new StringBuilder();
        if (parameters != null & parameters.size() != 0) {
            SortedMap<String, String> sortedParameters = new TreeMap<String, String>();

            for (Map.Entry<String, String> parameter : parameters.entrySet()) {
                sortedParameters.put(parameter.getKey(), RestUtil.urlEncode(parameter.getValue()));
            }
            StringBuilder sortedQueryString = new StringBuilder();
            for (Map.Entry<String, String> parameter : sortedParameters.entrySet()) {
                if (sortedQueryString != null && sortedQueryString.length() != 0)
                    sortedQueryString.append("&");
                sortedQueryString.append(parameter.getKey()).append("=");
                if (parameter.getValue() != null)
                    sortedQueryString.append(parameter.getValue());
            }
            queryString.append(sortedQueryString).append("\n");
        } else
            queryString.append("\n");
        return queryString.toString();
    }

    protected SortedMap<String, String> getCanonicalizedHeaders(Map<String, List<Object>> headers,
                                                                Map<String, String> parameters) {
        SortedMap<String, String> canonicalizedHeaders = new TreeMap<String, String>();

        for (String header : headers.keySet()) {
            String lcHeader = header.toLowerCase();
            if (!excludedSignedHeaders.contains(lcHeader))
                canonicalizedHeaders.put(lcHeader, trimAndJoin(headers.get(header), ","));
        }

        return canonicalizedHeaders;
    }

    protected String getStringToSign(String method, String resource, Map<String, String> parameters,
                                     Map<String, List<Object>> headers, String date, String service, String canonicalRequest) {
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append(S3Constants.AWS_HMAC_SHA256_ALGORITHM).append("\n");
        stringToSign.append(date).append("\n");
        SimpleDateFormat sdf = new SimpleDateFormat(AMZ_DATE_FORMAT, Locale.US);
        try {
            Date d = sdf.parse(date);
            sdf.applyPattern(AMZ_DATE_FORMAT_SHORT);
            date = sdf.format(d);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        stringToSign.append(getScope(date, service)).append("\n");

        // get hashedCanonicalRequest
        byte[] hash = hash256(canonicalRequest);
        String hashedRequest = hexEncode(hash);

        stringToSign.append(hashedRequest);
        return stringToSign.toString();
    }

    protected byte[] getSigningKey(String date, String service) {
        return hmac(S3Constants.HMAC_SHA_256,
                hmac(S3Constants.HMAC_SHA_256,
                        hmac(S3Constants.HMAC_SHA_256,
                                hmac(S3Constants.HMAC_SHA_256,
                                        (S3Constants.AWS_V4 + s3Config.getSecretKey()).getBytes(StandardCharsets.UTF_8), date),
                                S3Constants.AWS_DEFAULT_REGION),
                        service),
                S3Constants.AWS_V4_TERMINATOR
        );
    }

    protected String getDate(Map<String, String> parameters, Map<String, List<Object>> headers) {
        String date = null;
        // if x-amz-date is specified, date should be the value
        if (headers.containsKey(S3Constants.AMZ_DATE)) {
            date = RestUtil.getFirstAsString(headers, S3Constants.AMZ_DATE);
            return date;
        }
        // use Date header
        date = RestUtil.getFirstAsString(headers, RestUtil.HEADER_DATE);
        if (date == null) {
            // must have a date in the headers
            date = RestUtil.getRequestDate(s3Config.getServerClockSkew());
        }

        // convert date
        SimpleDateFormat sdf = new SimpleDateFormat(HEADER_DATE_FORMAT, Locale.US);
        try {
            Date d = sdf.parse(date);
            sdf.applyPattern(AMZ_DATE_FORMAT);
            return sdf.format(d);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getShortDate(String date) {
        // Date must be consistent with timestamp, so extract it
        // from previous date time format instead of get current date
        String shortDate = "";
        SimpleDateFormat sdf = new SimpleDateFormat(AMZ_DATE_FORMAT, Locale.US);
        try {
            Date d = sdf.parse(date);
            sdf.applyPattern(AMZ_DATE_FORMAT_SHORT);
            shortDate = sdf.format(d);
            return shortDate;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getScope(String shortDate, String service) {
        return shortDate + "/"
                + S3Constants.AWS_DEFAULT_REGION + "/"
                + service + "/"
                + S3Constants.AWS_V4_TERMINATOR;
    }

    protected String getSignature(String stringToSign, byte[] signingKey) {
        try {
            return hexEncode(hmac(S3Constants.HMAC_SHA_256, signingKey, stringToSign));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get getSignature");
        }
    }

    protected String getServiceType() {
        return S3Constants.AWS_SERVICE_S3;
    }

    public URL generatePresignedUrl(PresignedUrlRequest request) {
        String namespace = request.getNamespace() != null ? request.getNamespace() : s3Config.getNamespace();
        URI uri = s3Config.resolvePath(request.getPath(), null); // don't care about the query string yet
        // must construct both the final URL and the resource for signing
        String resource = "/" + request.getBucketName() + RestUtil.getEncodedPath(uri); // so here we have a uri encoding

        // insert namespace in host
        if (namespace != null) {
            if (s3Config.isUseVHost()) {
                uri = NamespaceFilter.insertNamespace(uri, namespace);
                if (s3Config.isSignNamespace())
                    resource = "/" + namespace + resource; // prepend to resource path for signing
            } else {
                // issue warning if namespace is specified and vhost is disabled because we can't put the namespace in the URL
                log.warn("vHost namespace is disabled, so there is no way to specify a namespace in a pre-signed URL");
            }
        }
        // insert bucket in host or path
        uri = BucketFilter.insertBucket(uri, request.getBucketName(), s3Config.isUseVHost());
        Map<String, String> parameters = new TreeMap();
        if (request.getVersionId() != null) parameters.put(S3Constants.PARAM_VERSION_ID, request.getVersionId());
        Map<ResponseHeaderOverride, String> headerOverrides = request.getHeaderOverrides();
        for (ResponseHeaderOverride override : headerOverrides.keySet()) {
            parameters.put(override.getQueryParam(), headerOverrides.get(override));
        }

        Map<String, List<Object>> headers = request.getHeaders();
        String method = request.getMethod().name();

        String date = null;
        String serviceType = getServiceType();
        if (headers.containsKey(S3Constants.AMZ_DATE)) {
            date = RestUtil.getFirstAsString(headers, S3Constants.AMZ_DATE);
        }
        else {
            date = getDate(parameters, headers);
        }
        String shortDate = getShortDate(date);

        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);
        StringBuilder signedHeaders = new StringBuilder();
        for (String name : canonicalizedHeaders.keySet()) {
            if (signedHeaders.length() != 0)
                signedHeaders.append(";");
            signedHeaders.append(name);
        }

        SortedMap<String, String> sortedParameters = new TreeMap();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            sortedParameters.put(parameter.getKey(), parameter.getValue());
        }

        sortedParameters.put("Action", method);
        sortedParameters.put("X-Amz-Algorithm", S3Constants.AWS_HMAC_SHA256_ALGORITHM);
        sortedParameters.put("X-Amz-Credential", RestUtil.urlDecode(s3Config.getIdentity() + "/" +
                shortDate + "/" + S3Constants.AWS_DEFAULT_REGION + "/" + serviceType + "/" +
                S3Constants.AWS_V4_TERMINATOR));
        sortedParameters.put("X-Amz-Date", date);
        sortedParameters.put("X-Amz-Expires", Long.toString(generateExpiration(request.getExpirationTime())));
        sortedParameters.put("X-Amz-SignedHeaders", RestUtil.urlDecode(signedHeaders.toString()));

        // #1 Create a canonical request for Signature Version 4
        String canonicalRequest = getCanonicalRequest(method, uri, sortedParameters, headers);

        // #2 Create a string to sign for Signature Version 4
        String stringToSign = getStringToSign(method, resource, parameters, headers, date, serviceType, canonicalRequest);
        log.debug("StringToSign: {}", stringToSign);

        // #3 Calculate the signature for AWS Signature Version 4
        byte[] key = getSigningKey(shortDate, serviceType);
        String signature = getSignature(stringToSign, key);
        log.debug("Signature: {}", signature);

        sortedParameters.put("X-Amz-Signature", signature);
        String rawQueryString = RestUtil.generateRawQueryString(sortedParameters);

        URI newUri = null;
        try {
            newUri = RestUtil.buildUri(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), rawQueryString, uri.getRawFragment());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            return new URL(newUri.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException("generated URL is not well-formed");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generated URL. ");
        }
    }

    private long generateExpiration(Date expirationDate) {
        long expirationInSeconds = expirationDate != null ? ((expirationDate
                .getTime() - System.currentTimeMillis()) / 1000L)
                : PRESIGN_URL_MAX_EXPIRATION_SECONDS;
        if (expirationInSeconds > PRESIGN_URL_MAX_EXPIRATION_SECONDS) {
            throw new IllegalArgumentException("A presigned URL can be valid for a maximum of seven days. " +
                    "The expiration date " + expirationInSeconds +
                    " set on the current request has exceeded this limit.");
        }
        return expirationInSeconds;
    }
}