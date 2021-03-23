package com.emc.object.s3;

import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.ClientRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class S3SignerV4 extends S3Signer{
    private static final String HEADER_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    //The timestamp must be in UTC and in the following ISO 8601 format: YYYYMMDD'T'HHMMSS'Z'
    private static final String AMZ_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    private static final String AMZ_DATE_FORMAT_SHORT = "yyyyMMdd";

    public S3SignerV4(S3Config s3Config) {
        super(s3Config);
    }

    public void sign(ClientRequest request, String resource, Map<String, String> parameters, Map<String, List<Object>> headers){
        // # Preparation, add x-amz-date and host headers
        String date = getDate(parameters, headers);
        String shortDate = getShortDate(date);
        String serviceType = getServiceType();
        request.getHeaders().add(S3Constants.AMZ_DATE, date);
        request.getHeaders().add(RestUtil.HEADER_HOST, s3Config.getHost());
        RestUtil.putSingle(headers,S3Constants.AMZ_DATE, date);
        RestUtil.putSingle(headers,RestUtil.HEADER_HOST, s3Config.getHost());
        headers = request.getHeaders();
        // #1 Create a canonical request for Signature Version 4
        String canonicalRequest = getCanonicalRequest(request, parameters, headers);

        // #2 Create a string to sign for Signature Version 4
        // get HashedCanonicalRequest
        byte[] hash = hash256(canonicalRequest);
        String hashedRequest = hexEncode(hash);
        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);
        StringBuffer signedHeaders = new StringBuffer();
        for (String name : canonicalizedHeaders.keySet()) {
            if(signedHeaders.length() != 0)
                signedHeaders.append(";");
            signedHeaders.append(name);
        }
        String stringToSign = getStringToSign(request.getMethod(), resource, parameters, headers, date, serviceType) + hashedRequest;
        log.debug("StringToSign: {}", stringToSign);

        // #3 Calculate the signature for AWS Signature Version 4
        byte[] key = getSigningKey(shortDate, serviceType);
        String signature = getSignature(stringToSign, key);
        log.debug("Signature: {}", signature);

        // #4 Adding signing information to the authorization header
        RestUtil.putSingle(headers,"Authorization",S3Constants.AWS_HMAC_SHA256_ALGORITHM +
                " Credential=" + s3Config.getIdentity() + "/" + shortDate +
                "/" + S3Constants.AWS_DEFAULT_REGION + "/" + serviceType + "/" + S3Constants.AWS_V4_TERMINATOR +
                ", SignedHeaders=" + signedHeaders.toString() + ", " + S3Constants.PARAM_SIGNATURE + "= " + signature);
    }

    protected String getCanonicalRequest(ClientRequest request, Map<String, String> parameters, Map<String, List<Object>> headers)
    {
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
        if(parameters != null & parameters.size() != 0)
        {
            SortedMap<String, String> sortedParameters = new TreeMap<String, String>();

            for(Map.Entry<String, String> parameter : parameters.entrySet()) {
                sortedParameters.put(parameter.getKey(), parameter.getValue());
            }
            StringBuilder sortedQueryString = new StringBuilder();
            for(Map.Entry<String, String> parameter: sortedParameters.entrySet()) {
                if(sortedQueryString != null && sortedQueryString.length() != 0)
                    sortedQueryString.append("&");
                sortedQueryString.append(parameter.getKey()).append("=");
                if(parameter.getValue()!= null)
                    sortedQueryString.append(parameter.getValue());
            }
            canonicalRequest.append(sortedQueryString).append("\n");
        }
        else
            canonicalRequest.append("\n");

        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);

        StringBuffer signedHeaders = new StringBuffer();
        for (String name : canonicalizedHeaders.keySet()) {
            canonicalRequest.append(name).append(":").append(canonicalizedHeaders.get(name).trim());
            canonicalRequest.append("\n");
            if(signedHeaders.length() != 0)
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

    protected String getStringToSign(String method, String resource, Map<String, String> parameters,
                           Map<String, List<Object>> headers, String date, String service) {
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

        String stringToSignStr = stringToSign.toString();
        return stringToSignStr;
    }

    protected byte[] getSigningKey(String date, String service){
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

    protected String getDate(Map<String, String> parameters, Map<String, List<Object>> headers){
        // date line
        // use Date header by default
        String date = RestUtil.getFirstAsString(headers, RestUtil.HEADER_DATE);
        if (date == null) {
            // must have a date in the headers
            date = RestUtil.getRequestDate(s3Config.getServerClockSkew());
        }
        // if x-amz-date is specified, date line should be blank
        if (headers.containsKey(S3Constants.AMZ_DATE))
            date = "";
        // if expires parameter is set, use that instead
        if (parameters.containsKey(S3Constants.PARAM_EXPIRES))
            date = parameters.get(S3Constants.PARAM_EXPIRES);

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
        String shoartDate = "";
        SimpleDateFormat sdf = new SimpleDateFormat(AMZ_DATE_FORMAT, Locale.US);
        try {
            Date d = sdf.parse(date);
            sdf.applyPattern(AMZ_DATE_FORMAT_SHORT);
            shoartDate = sdf.format(d);
            return shoartDate;
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

    protected String getSignature(String stringToSign, byte[] signingKey)  {
        try {
            return hexEncode(hmac(S3Constants.HMAC_SHA_256, signingKey, stringToSign));
        }
        catch(Exception e)
        {
            return "";
        }
    }

    protected String getServiceType() {
        return S3Constants.AWS_SERVICE_S3;
    }
}