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

public final class S3SignerV2 extends S3Signer {

    public S3SignerV2(S3Config s3Config) {
        super(s3Config);
    }

    public void sign(String method, String resource, Map<String, String> parameters, Map<String, List<Object>> headers) {
        String stringToSign = getStringToSign(method, resource, parameters, headers);
        String signature = getSignature(stringToSign);
        RestUtil.putSingle(headers, "Authorization", "AWS " + s3Config.getIdentity() + ":" + signature);
    }

    protected String getSignature(String stringToSign) {
        return hmac(S3Constants.HMAC_SHA_1, s3Config.getSecretKey(), stringToSign);
    }

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
}
