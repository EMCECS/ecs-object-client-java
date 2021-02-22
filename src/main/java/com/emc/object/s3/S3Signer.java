package com.emc.object.s3;

import com.emc.object.s3.jersey.BucketFilter;
import com.emc.object.s3.jersey.NamespaceFilter;
import com.emc.object.s3.request.PresignedUrlRequest;
import com.emc.object.util.RestUtil;
import com.sun.xml.bind.v2.runtime.reflect.Lister;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class S3Signer {
    protected static final Logger log = LoggerFactory.getLogger(S3Signer.class);

    protected S3Config s3Config;
    protected SortedSet<String> signedParameters;

    S3Signer(S3Config s3Config) {
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
                S3Constants.PARAM_IS_STALE_ALLOWED));
        if (s3Config.isSignMetadataSearch()) {
            signedParameters.add(S3Constants.PARAM_QUERY);
            signedParameters.add(S3Constants.PARAM_SEARCH_METADATA);
        }
    }

    public abstract void sign(String method, String resource, Map<String, String> parameters,
                              Map<String, List<Object>> headers);

    protected abstract String getSignature(String stringToSign);

    protected abstract String getDate(Map<String, String> parameters, Map<String, List<Object>> headers);

    // generalized utility function to get hmac values
    // NOTE: I do not know if the constant value "HmacSHA256" is actually valid for this (though I suspect it is)
    // be sure to confirm that "HmacSHA256" works before continuing too long
    protected String hmac(String algorithm, String var1, String var2) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(var1.getBytes("UTF-8"), algorithm));
            String result = new String(Base64.encodeBase64(mac.doFinal(var2.getBytes("UTF-8"))));
            log.debug("hmac of {} and {}:\n{}", var1, var2, result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + " algorithm is not supported on this platform", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported on this platform", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("The secret key \"" + s3Config.getSecretKey() + "\" is not valid", e);
        }
    }

    // HERE: we need a SHA256 hashing function that is not HMAC like above (no key)
    protected String hash256(Object toHash) {

        return toHash.toString();
    }

    protected String getCanonicalizedQueryString(PresignedUrlRequest request, Map<String, String> queryParams) {
        // does the request have a sub-resource (i.e. ?acl)?
        String subresource = request.getSubresource() != null ? request.getSubresource() + "&" : "";
        // we must manually append the query string to ensure nothing is re-encoded
        return "?" + subresource + RestUtil.generateRawQueryString(queryParams);
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

        // build parameters
        // it doesn't look like this is what we need quite
        // parameters need to be encoded in v4 -- let's see if they needed to be encoded in v2
        Map<String, String> queryParams = request.getQueryParams();
        queryParams.put(S3Constants.PARAM_ACCESS_KEY, s3Config.getIdentity());

        // sign the request
        String stringToSign = getStringToSign(request.getMethod().toString(), resource, queryParams,
                request.getHeaders());
        String signature = getSignature(stringToSign);

        // add signature to query string
        queryParams.put(S3Constants.PARAM_SIGNATURE, signature);

        try {
            return new URL(uri + getCanonicalizedQueryString(request, queryParams));
        } catch (MalformedURLException e) {
            throw new RuntimeException("generated URL is not well-formed");
        }
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

    /* *
     * encode String to hex - required for v4 auth
     * */
    protected String hexEncode(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes()));
    }

    protected String trimAndJoin(List<Object> values, String delimiter) {
        if (values == null || values.isEmpty()) return null;
        StringBuilder delimited = new StringBuilder();
        Iterator<Object> valuesI = values.iterator();
        while (valuesI.hasNext()) {
            delimited.append(valuesI.next().toString().trim());
            if (valuesI.hasNext()) delimited.append(delimiter);
        }
        return delimited.toString();
    }
}