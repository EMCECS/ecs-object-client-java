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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.xml.bind.DatatypeConverter;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3Signer;
import com.emc.object.s3.S3SignerV2;
import com.emc.object.s3.S3SignerV4;
import com.emc.object.s3.VHostUtil;
import com.emc.object.util.ChecksumAlgorithm;
import com.emc.object.util.ChecksumValueImpl;
import com.emc.object.util.ChecksummedInputStream;
import com.emc.object.util.ChecksummedOutputStream;
import com.emc.object.util.RestUtil;
import com.emc.object.util.RunningChecksum;

@Provider
public class ChecksumFilter implements WriterInterceptor, ReaderInterceptor {
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
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        Boolean verifyWrite = (Boolean) context.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
        Boolean generateMd5 = (Boolean) context.getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5);

        if ((verifyWrite != null && verifyWrite) || (generateMd5 != null && generateMd5)) {
            try {
                RunningChecksum checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                OutputStream originalStream = context.getOutputStream();
                
                if (generateMd5 != null && generateMd5) {
                    // Buffer the output to calculate MD5 before writing
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    ChecksummedOutputStream checksumStream = new ChecksummedOutputStream(buffer, checksum);
                    context.setOutputStream(checksumStream);
                    context.proceed();
                    
                    // Add Content-MD5 header
                    context.getHeaders().putSingle(RestUtil.HEADER_CONTENT_MD5,
                            DatatypeConverter.printBase64Binary(checksum.getByteValue()));
                    
                    // Re-sign request if needed
                    if (s3Config.getIdentity() != null) {
                        String resource = VHostUtil.getResourceString(s3Config,
                                (String) context.getProperty(RestUtil.PROPERTY_NAMESPACE),
                                (String) context.getProperty(S3Constants.PROPERTY_BUCKET_NAME),
                                "/"); // Path needs to be extracted from context
                        // Note: Re-signing in interceptor is complex - may need request context
                    }
                    
                    // Write buffered data to original stream
                    originalStream.write(buffer.toByteArray());
                } else if (verifyWrite != null && verifyWrite) {
                    // Wrap stream to calculate checksum
                    ChecksummedOutputStream checksumStream = new ChecksummedOutputStream(originalStream, checksum);
                    context.setOutputStream(checksumStream);
                    context.proceed();
                    
                    // Store checksum for verification after response
                    context.setProperty("checksum.write.value", checksum.getHexValue());
                }
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("fatal: MD5 algorithm not found", e);
            }
        } else {
            context.proceed();
        }
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        Boolean verifyRead = (Boolean) context.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM);
        
        if (verifyRead != null && verifyRead) {
            try {
                // Pull etag from response headers
                String md5Header = context.getHeaders().getFirst(RestUtil.HEADER_ETAG);
                if (md5Header != null) md5Header = md5Header.replaceAll("\"", "");
                if (md5Header != null && (md5Header.length() <= 2 || md5Header.contains("-")))
                    md5Header = null; // look for valid etags

                // Also look for content MD5 (this trumps etag if present)
                String contentMd5 = context.getHeaders().getFirst(RestUtil.EMC_CONTENT_MD5);
                if (contentMd5 != null) md5Header = contentMd5;
                
                if (md5Header != null) {
                    // Wrap stream to verify read checksum
                    InputStream originalStream = context.getInputStream();
                    ChecksummedInputStream checksumStream = new ChecksummedInputStream(originalStream,
                            new ChecksumValueImpl(ChecksumAlgorithm.MD5, 0, md5Header));
                    context.setInputStream(checksumStream);
                }
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("fatal: MD5 algorithm not found", e);
            }
        }
        
        return context.proceed();
    }

}
