/*
 * Copyright (c) 2015-2016, EMC Corporation.
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
package com.emc.object.s3;

import com.emc.object.s3.jersey.BucketFilter;
import com.emc.object.s3.jersey.NamespaceFilter;
import com.emc.object.s3.request.PresignedUrlRequest;
import com.emc.object.util.RestUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class S3SignerV2 {
    private static final Logger log = LoggerFactory.getLogger(S3SignerV2.class);

    private S3Config s3Config;
    private SortedSet<String> signedParameters;

    public S3SignerV2(S3Config s3Config) {
        this.s3Config = s3Config;
        signedParameters = new TreeSet<String>(Arrays.asList(
                "acl", "torrent", "logging", "location", "policy", "requestPayment", "versioning",
                "versions", "versionId", "notification", "uploadId", "uploads", "partNumber", "website",
                "delete", "lifecycle", "tagging", "cors", "restore",
                S3Constants.PARAM_RESPONSE_HEADER_CACHE_CONTROL,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_DISPOSITION,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_ENCODING,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_LANGUAGE,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_TYPE,
                S3Constants.PARAM_RESPONSE_HEADER_EXPIRES,
                S3Constants.PARAM_ENDPOINT,
                S3Constants.PARAM_IS_STALE_ALLOWED,
                S3Constants.PARAM_SECURITY_TOKEN));
        if (s3Config.isSignMetadataSearch()) {
            signedParameters.add(S3Constants.PARAM_QUERY);
            signedParameters.add(S3Constants.PARAM_SEARCH_METADATA);
        }
    }

    public void sign(String method, String resource, Map<String, String> parameters, Map<String, List<Object>> headers) {

        if (s3Config.getSessionToken() != null) {
            parameters.put(S3Constants.PARAM_SECURITY_TOKEN, s3Config.getSessionToken());
        }
        String stringToSign = getStringToSign(method, resource, parameters, headers);
        String signature = getSignature(stringToSign);
        RestUtil.putSingle(headers, "Authorization", "AWS " + s3Config.getIdentity() + ":" + signature);
    }

    public URL generatePresignedUrl(PresignedUrlRequest request) {
        String namespace = request.getNamespace() != null ? request.getNamespace() : s3Config.getNamespace();

        URI uri = s3Config.resolvePath(request.getPath(), null); // don't care about the query string yet

        // must construct both the final URL and the resource for signing
        String resource = "/" + request.getBucketName() + RestUtil.getEncodedPath(uri);

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

        // build parameters
        Map<String, String> queryParams = request.getQueryParams();
        queryParams.put(S3Constants.PARAM_ACCESS_KEY, s3Config.getIdentity());

        if (s3Config.getSessionToken() != null) {
            queryParams.put(S3Constants.AMZ_SECURITY_TOKEN, s3Config.getSessionToken());
        }

        // sign the request
        String stringToSign = getStringToSign(request.getMethod().toString(), resource, queryParams, request.getHeaders());
        String signature = getSignature(stringToSign);

        // add signature to query string
        queryParams.put(S3Constants.PARAM_SIGNATURE, signature);

        // does the request have a sub-resource (i.e. ?acl)?
        String subresource = request.getSubresource() != null ? request.getSubresource() + "&" : "";

        try {
            // we must manually append the query string to ensure nothing is re-encoded
            return new URL(uri + "?" + subresource + RestUtil.generateRawQueryString(queryParams));
        } catch (MalformedURLException e) {
            throw new RuntimeException("generated URL is not well-formed");
        }
    }

    public String getStringToSign(String method, String resource, Map<String, String> parameters,
                                  Map<String, List<Object>> headers) {
        StringBuilder stringToSign = new StringBuilder();

        // method line
        stringToSign.append(method).append("\n");

        // MD5 line
        String contentMd5 = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_MD5);
        if (contentMd5 != null) stringToSign.append(contentMd5);
        stringToSign.append("\n");

        // content type line
        String contentType = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_TYPE);
        if (contentType != null) stringToSign.append(contentType);
        stringToSign.append("\n");

        // date line
        // use Date header by default
        String date = RestUtil.getFirstAsString(headers, RestUtil.HEADER_DATE);
        if (date == null) {
            // must have a date in the headers
            date = RestUtil.getRequestDate(s3Config.getServerClockSkew());
            RestUtil.putSingle(headers, RestUtil.HEADER_DATE, date);
        }
        // if x-amz-date is specified, date line should be blank
        if (headers.containsKey(S3Constants.AMZ_DATE))
            date = "";
        // if expires parameter is set, use that instead
        if (parameters.containsKey(S3Constants.PARAM_EXPIRES))
            date = parameters.get(S3Constants.PARAM_EXPIRES);
        stringToSign.append(date);
        stringToSign.append("\n");

        // canonicalized headers
        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);
        for (String name : canonicalizedHeaders.keySet()) {
            stringToSign.append(name).append(":").append(canonicalizedHeaders.get(name).trim());
            stringToSign.append("\n");
        }

        // resource path (includes signed parameters)
        stringToSign.append(resource);
        boolean firstParameter = true;
        for (String parameter : signedParameters) {
            if (parameters.containsKey(parameter)) {
                stringToSign.append(firstParameter ? "?" : "&").append(parameter);
                String value = parameters.get(parameter);
                if (value != null) stringToSign.append("=").append(value);
                firstParameter = false;
            }
        }

        String stringToSignStr = stringToSign.toString();
        log.debug("stringToSign:\n" + stringToSignStr);
        return stringToSignStr;
    }

    private SortedMap<String, String> getCanonicalizedHeaders(Map<String, List<Object>> headers, Map<String, String> parameters) {
        SortedMap<String, String> canonicalizedHeaders = new TreeMap<String, String>();

        // add x-emc- and x-amz- headers
        for (String header : headers.keySet()) {
            String lcHeader = header.toLowerCase();
            if (lcHeader.startsWith(S3Constants.AMZ_PREFIX) || lcHeader.startsWith(RestUtil.EMC_PREFIX)) {
                canonicalizedHeaders.put(lcHeader, trimAndJoin(headers.get(header), ","));
            }
        }

        // add x-amz- parameters
        for (String parameter : parameters.keySet()) {
            String lcParameter = parameter.toLowerCase();
            if (lcParameter.startsWith(S3Constants.AMZ_PREFIX)) {
                canonicalizedHeaders.put(lcParameter, parameters.get(parameter));
            }
        }

        return canonicalizedHeaders;
    }

    private String trimAndJoin(List<Object> values, String delimiter) {
        if (values == null || values.isEmpty()) return null;
        StringBuilder delimited = new StringBuilder();
        Iterator<Object> valuesI = values.iterator();
        while (valuesI.hasNext()) {
            delimited.append(valuesI.next().toString().trim());
            if (valuesI.hasNext()) delimited.append(delimiter);
        }
        return delimited.toString();
    }

    public String getSignature(String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(s3Config.getSecretKey().getBytes("UTF-8"), "HmacSHA1")); // AWS does not B64-decode the secret key!
            String signature = new String(Base64.encodeBase64(mac.doFinal(stringToSign.getBytes("UTF-8"))));
            log.debug("signature:\n" + signature);
            return signature;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HmacSHA1 algorithm is not supported on this platform", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported on this platform", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("The secret key \"" + s3Config.getSecretKey() + "\" is not valid", e);
        }
    }
}
