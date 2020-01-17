package com.emc.object.s3;

import com.emc.object.util.RestUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class S3SignerV4 extends S3Signer{
    private static final String HEADER_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    private static final String V4_DATE_FORMAT = "yyyyMMdd";

    /*
    * NOTES:
    * it looks like
     */

    public S3SignerV4(S3Config s3Config) {
        super(s3Config);
    }

    public void sign(String method, String resource, Map<String, String> parameters, Map<String, List<Object>> headers){
        // I can add required v4 headers here and then continue on my way
        // what headers are those? And how do i properly put them into this map<string, List<>>?
        // Stu made us a utility to do this in RestUtil
        // still need to SHA256 the request body...
        RestUtil.putSingle(headers, S3Constants.AMZ_CONTENT_SHA256, hexEncode("something")); // where do i get the content to encode from?
        String stringToSign = getStringToSign(method, resource, parameters, headers);
    }

    // this is gross - but probably the best way? either way it comes out the same, might be more readable if broken out
    // NOTE: getDate() is not complete
    // NOTE: where some of the strings are written there should either be getters or constants
    // However, this is more or less what this function will look like when complete
    private String getSigningKey(){
        return hmac(S3Constants.HMAC_SHA_256,
            hmac(S3Constants.HMAC_SHA_256,
                hmac(S3Constants.HMAC_SHA_256,
                    hmac(S3Constants.HMAC_SHA_256,
                            (S3Constants.AWS_V4 + s3Config.getSecretKey()), getDate()),
                        S3Constants.AWS_DEFAULT_REGION),
                    S3Constants.AWS_SERVICE_S3),
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
            RestUtil.putSingle(headers, RestUtil.HEADER_DATE, date); // abstract formatDate(date) here implemented by children
        }
        // if x-amz-date is specified, date line should be blank
        if (headers.containsKey(S3Constants.AMZ_DATE))
            date = "";
        // if expires parameter is set, use that instead
        if (parameters.containsKey(S3Constants.PARAM_EXPIRES))
            date = parameters.get(S3Constants.PARAM_EXPIRES);

        // convert date
        // this makes a deep assumption on the formatting of the date, i need to be sure that is correct
        // obviously this will only work if the date comes in the form of "Tue, 27 Mar 2007 19:36:42 +0000"
        // this _may_ be safe to assume, i think this assumption is already being made
        SimpleDateFormat sdf = new SimpleDateFormat(HEADER_DATE_FORMAT, Locale.US);
        try {
            Date d = sdf.parse(date);
            sdf.applyPattern(V4_DATE_FORMAT);
            return sdf.format(d);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getDate() {
         return "date";
    }

    protected String getScope(Map<String, String> parameters, Map<String, List<Object>> headers) {
       return getDate(parameters, headers) + "/"
               + S3Constants.AWS_DEFAULT_REGION + "/"
               + S3Constants.AWS_SERVICE_S3 + "/"
               + S3Constants.AWS_V4_TERMINATOR;
    }

    // this is not complete and will require additional changes for v4
    protected String getSignature(String stringToSign) {
        return hexEncode(hmac(S3Constants.HMAC_SHA_256, getSigningKey(), stringToSign));
    }
}
