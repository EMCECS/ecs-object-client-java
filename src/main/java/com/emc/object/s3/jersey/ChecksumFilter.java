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
package com.emc.object.s3.jersey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.xml.bind.DatatypeConverter;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Signer;
import com.emc.object.s3.S3SignerV2;
import com.emc.object.s3.S3SignerV4;
import com.emc.object.util.ChecksumAlgorithm;
import com.emc.object.util.ChecksumError;
import com.emc.object.util.ChecksumValueImpl;
import com.emc.object.util.ChecksummedInputStream;
import com.emc.object.util.ChecksummedOutputStream;
import com.emc.object.util.RestUtil;
import com.emc.object.util.RunningChecksum;

public class ChecksumFilter implements WriterInterceptor, ClientResponseFilter {
    private static final String PROP_WRITE_CHECKSUM = "com.emc.object.checksumFilter.writeChecksum";

    private S3Config s3Config;
    private S3Signer signer;

    public ChecksumFilter(S3Config s3Config) {
        this.s3Config = s3Config;
        if(s3Config.isUseV2Signer())
            this.signer = new S3SignerV2(s3Config);
        else
            this.signer = new S3SignerV4(s3Config);
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        Boolean verifyWrite = (Boolean) context.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
        Boolean generateMd5 = (Boolean) context.getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5);

        RunningChecksum checksum = null;
        OutputStream originalStream = context.getOutputStream();

        try {
            if (verifyWrite != null && verifyWrite) {
                checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                context.setOutputStream(new ChecksummedOutputStream(context.getOutputStream(), checksum));
                context.setProperty(PROP_WRITE_CHECKSUM, checksum);
            }

            if (generateMd5 != null && generateMd5) {
                // buffer the entity to calculate MD5 before sending
                RunningChecksum md5Checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                context.setOutputStream(new ChecksummedOutputStream(buffer, md5Checksum));
                context.proceed();

                // add Content-MD5 header (this mutates the outbound headers)
                context.getHeaders().putSingle(RestUtil.HEADER_CONTENT_MD5,
                        DatatypeConverter.printBase64Binary(md5Checksum.getByteValue()));

                // re-sign if credentials were available at request time (the Content-MD5 header
                // is part of the V2 stringToSign and part of the V4 canonical headers, so the
                // signature computed earlier by AuthorizationFilter is now stale)
                if (s3Config.getIdentity() != null) {
                    S3Signer stashedSigner = (S3Signer) context.getProperty(com.emc.object.s3.jersey.AuthorizationFilter.PROP_SIGNER);
                    if (stashedSigner != null) {
                        String method = (String) context.getProperty(com.emc.object.s3.jersey.AuthorizationFilter.PROP_SIGN_METHOD);
                        java.net.URI uri = (java.net.URI) context.getProperty(com.emc.object.s3.jersey.AuthorizationFilter.PROP_SIGN_URI);
                        String resource = (String) context.getProperty(com.emc.object.s3.jersey.AuthorizationFilter.PROP_SIGN_RESOURCE);
                        @SuppressWarnings("unchecked")
                        Map<String, String> parameters = (Map<String, String>) context.getProperty(com.emc.object.s3.jersey.AuthorizationFilter.PROP_SIGN_PARAMETERS);
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Map<String, List<Object>> signingHeaders = (Map) context.getHeaders();
                        stashedSigner.resign(method, uri, resource, parameters, signingHeaders);
                    }
                }

                // write buffered data to original stream
                originalStream.write(buffer.toByteArray());
                return; // already proceeded
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal: MD5 algorithm not found");
        }

        context.proceed();
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        // pull etag from response headers
        String md5Header = RestUtil.getFirstAsString(responseContext.getHeaders(), RestUtil.HEADER_ETAG);
        if (md5Header != null) md5Header = md5Header.replaceAll("\"", "");
        if (md5Header != null && (md5Header.length() <= 2 || md5Header.contains("-")))
            md5Header = null; // look for valid etags

        // also look for content MD5 (this trumps etag if present)
        String contentMd5 = RestUtil.getFirstAsString(responseContext.getHeaders(), RestUtil.EMC_CONTENT_MD5);
        if (contentMd5 != null) md5Header = contentMd5;

        Boolean verifyWrite = (Boolean) requestContext.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
        if (verifyWrite != null && verifyWrite && md5Header != null) {
            RunningChecksum checksum = (RunningChecksum) requestContext.getProperty(PROP_WRITE_CHECKSUM);
            if (checksum != null && !checksum.getHexValue().equals(md5Header))
                throw new ChecksumError("Checksum failure while writing stream", checksum.getHexValue(), md5Header);
        }

        Boolean verifyRead = (Boolean) requestContext.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM);
        if (verifyRead != null && verifyRead && md5Header != null) {
            try {
                // wrap stream to verify read checksum
                responseContext.setEntityStream(new ChecksummedInputStream(responseContext.getEntityStream(),
                        new ChecksumValueImpl(ChecksumAlgorithm.MD5, 0, md5Header))); // won't have length for chunked responses
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }
    }
}
