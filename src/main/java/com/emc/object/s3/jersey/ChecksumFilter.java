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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.xml.bind.DatatypeConverter;

import org.glassfish.jersey.client.ClientRequest;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3Signer;
import com.emc.object.s3.S3SignerV2;
import com.emc.object.s3.S3SignerV4;
import com.emc.object.s3.VHostUtil;
import com.emc.object.util.ChecksumAlgorithm;
import com.emc.object.util.ChecksumError;
import com.emc.object.util.ChecksumValueImpl;
import com.emc.object.util.ChecksummedInputStream;
import com.emc.object.util.ChecksummedOutputStream;
import com.emc.object.util.RestUtil;
import com.emc.object.util.RunningChecksum;

public class ChecksumFilter implements ClientRequestFilter, ClientResponseFilter, WriterInterceptor {
    static final String PROP_VERIFY_WRITE = "com.emc.object.checksumFilter.verifyWrite";
    static final String PROP_VERIFY_READ = "com.emc.object.checksumFilter.verifyRead";
    static final String PROP_GENERATE_MD5 = "com.emc.object.checksumFilter.generateMd5";
    static final String PROP_WRITE_CHECKSUM = "com.emc.object.checksumFilter.writeChecksum";

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
    public void filter(ClientRequestContext requestContext) throws IOException {
        // propagate checksum flags to request properties for use in WriterInterceptor and response filter
        Boolean verifyWrite = (Boolean) requestContext.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
        if (verifyWrite != null && verifyWrite) {
            requestContext.setProperty(PROP_VERIFY_WRITE, true);
        }
        Boolean generateMd5 = (Boolean) requestContext.getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5);
        if (generateMd5 != null && generateMd5) {
            requestContext.setProperty(PROP_GENERATE_MD5, true);
        }
        Boolean verifyRead = (Boolean) requestContext.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM);
        if (verifyRead != null && verifyRead) {
            requestContext.setProperty(PROP_VERIFY_READ, true);
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        Boolean verifyWrite = (Boolean) context.getProperty(PROP_VERIFY_WRITE);
        Boolean generateMd5 = (Boolean) context.getProperty(PROP_GENERATE_MD5);

        RunningChecksum writeChecksum = null;
        OutputStream originalOut = context.getOutputStream();

        try {
            if (generateMd5 != null && generateMd5) {
                // buffer the entity, compute MD5, add Content-MD5 header, re-sign, then write
                RunningChecksum md5Checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                OutputStream checksumOut = new ChecksummedOutputStream(buffer, md5Checksum);
                context.setOutputStream(checksumOut);
                context.proceed();
                checksumOut.close();

                // add Content-MD5 header
                context.getHeaders().putSingle(RestUtil.HEADER_CONTENT_MD5,
                        DatatypeConverter.printBase64Binary(md5Checksum.getByteValue()));

                // re-sign request because Content-MD5 is included in the signature
                ClientRequest request = (ClientRequest) context.getProperty("com.emc.object.clientRequest");
                if (request != null && s3Config.getIdentity() != null) {
                    Map<String, String> parameters = RestUtil.getQueryParameterMap(request.getUri().getRawQuery());
                    String resource = VHostUtil.getResourceString(s3Config,
                            (String) request.getProperty(RestUtil.PROPERTY_NAMESPACE),
                            (String) request.getProperty(S3Constants.PROPERTY_BUCKET_NAME),
                            RestUtil.getEncodedPath(request.getUri()));
                    signer.sign(request, resource, parameters, request.getHeaders());
                }

                // also track write checksum if needed
                if (verifyWrite != null && verifyWrite) {
                    writeChecksum = md5Checksum;
                    context.setProperty(PROP_WRITE_CHECKSUM, writeChecksum);
                }

                // write buffered data to original stream
                originalOut.write(buffer.toByteArray());
                return;
            }

            if (verifyWrite != null && verifyWrite) {
                // wrap stream to calculate write checksum
                writeChecksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                context.setOutputStream(new ChecksummedOutputStream(originalOut, writeChecksum));
                context.setProperty(PROP_WRITE_CHECKSUM, writeChecksum);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal: MD5 algorithm not found");
        }

        context.proceed();
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        // pull etag from response headers
        List<String> etagHeaders = responseContext.getHeaders().get(RestUtil.HEADER_ETAG);
        String md5Header = (etagHeaders != null && !etagHeaders.isEmpty()) ? etagHeaders.get(0) : null;
        if (md5Header != null) md5Header = md5Header.replaceAll("\"", "");
        if (md5Header != null && (md5Header.length() <= 2 || md5Header.contains("-")))
            md5Header = null; // look for valid etags

        // also look for content MD5 (this trumps etag if present)
        List<String> contentMd5Headers = responseContext.getHeaders().get(RestUtil.EMC_CONTENT_MD5);
        String contentMd5 = (contentMd5Headers != null && !contentMd5Headers.isEmpty()) ? contentMd5Headers.get(0) : null;
        if (contentMd5 != null) md5Header = contentMd5;

        Boolean verifyWrite = (Boolean) requestContext.getProperty(PROP_VERIFY_WRITE);
        if (verifyWrite != null && verifyWrite && md5Header != null) {
            RunningChecksum writeChecksum = (RunningChecksum) requestContext.getProperty(PROP_WRITE_CHECKSUM);
            if (writeChecksum != null && !writeChecksum.getHexValue().equals(md5Header))
                throw new ChecksumError("Checksum failure while writing stream", writeChecksum.getHexValue(), md5Header);
        }

        Boolean verifyRead = (Boolean) requestContext.getProperty(PROP_VERIFY_READ);
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
