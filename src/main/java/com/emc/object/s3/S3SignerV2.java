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

import javax.ws.rs.client.ClientRequestContext;
import javax.xml.bind.DatatypeConverter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class S3SignerV2 extends S3Signer {
    private SortedSet<String> signedParameters;

    public S3SignerV2(S3Config s3Config) {
        super(s3Config);
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
                S3Constants.PARAM_IS_STALE_ALLOWED));
        if (s3Config.isSignMetadataSearch()) {
            signedParameters.add(S3Constants.PARAM_QUERY);
            signedParameters.add(S3Constants.PARAM_SEARCH_METADATA);
        }
    }

    @Override
    public void sign(ClientRequestContext request, String resource, Map<String, String> parameters, Map<String, List<Object>> headers) {
        if (s3Config.getSessionToken() != null) {
            RestUtil.putSingle(headers, S3Constants.AMZ_SECURITY_TOKEN, s3Config.getSessionToken());
        }
        String stringToSign = getStringToSign(request.getMethod(), resource, parameters, headers);
        String signature = getSignature(stringToSign, null);
        RestUtil.putSingle(headers, "Authorization", "AWS " + s3Config.getIdentity() + ":" + signature);
    }

    @Override
    protected String getSignature(String stringToSign, byte[] signingKey) {
        return DatatypeConverter.printBase64Binary(
                hmac(S3Constants.HMAC_SHA_1,
                        s3Config.getSecretKey().getBytes(StandardCharsets.UTF_8),
                        stringToSign));
    }

    @Override
    protected String getDate(Map<String, String> parameters, Map<String, List<Object>> headers) {
        // date line
        // use Date header by default
        String date = RestUtil.getFirstAsString(headers, RestUtil.HEADER_DATE);
        if (date == null) {
            // must have a date in the headers
            date = RestUtil.getRequestDate(s3Config.getServerClockSkew());
            RestUtil.putSingle(headers, RestUtil.HEADER_DATE, date); // abstract formatDate(date) here implemented by children
        }
        // if x-amz-date is specified, date line should be blank
        if (headers.containsKey(S3Constants.AMZ_DATE))
            date = "";
        // if expires parameter is set, use that instead
        if (parameters.containsKey(S3Constants.PARAM_EXPIRES))
            date = parameters.get(S3Constants.PARAM_EXPIRES);
        return date;
    }

    @Override
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

        // build parameters
        // it doesn't look like this is what we need quite
        // parameters need to be encoded in v4 -- let's see if they needed to be encoded in v2
        Map<String, String> queryParams = request.getQueryParams();
        queryParams.put(S3Constants.PARAM_ACCESS_KEY, s3Config.getIdentity());

        if (s3Config.getSessionToken() != null) {
            queryParams.put(S3Constants.AMZ_SECURITY_TOKEN, s3Config.getSessionToken());
        }

        // sign the request
        String stringToSign = getStringToSign(request.getMethod().toString(), resource, queryParams,
                request.getHeaders());
        String signature = getSignature(stringToSign, null);

        // add signature to query string
        queryParams.put(S3Constants.PARAM_SIGNATURE, signature);

        try {
            return new URL(uri + getCanonicalizedQueryString(request, queryParams));
        } catch (MalformedURLException e) {
            throw new RuntimeException("generated URL is not well-formed");
        }
    }

    private String getCanonicalizedQueryString(PresignedUrlRequest request, Map<String, String> queryParams) {
        // does the request have a sub-resource (i.e. ?acl)?
        String subresource = request.getSubresource() != null ? request.getSubresource() + "&" : "";
        // we must manually append the query string to ensure nothing is re-encoded
        return "?" + subresource + RestUtil.generateRawQueryString(queryParams);
    }

    String getStringToSign(String method, String resource, Map<String, String> parameters,
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

        // add date (must be implemented in concrete class)
        stringToSign.append(getDate(parameters, headers));
        stringToSign.append("\n");

        // canonicalized headers
        // signature v4 requires at least one more header - consider externalizing to abstract function?
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

    @Override
    protected SortedMap<String, String> getCanonicalizedHeaders(Map<String, List<Object>> headers,
                                                                Map<String, String> parameters) {
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
}
