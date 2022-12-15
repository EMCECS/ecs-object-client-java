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

import com.emc.jersey.AbstractClientRequestAdapter;
import com.emc.jersey.ClientRequestAdapter;
import com.emc.object.s3.*;
import com.emc.object.util.*;
import org.glassfish.jersey.client.ClientRequest;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import javax.ws.rs.ext.Provider;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Provider
public class ChecksumFilter implements ClientRequestFilter, ClientResponseFilter {
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
    public void filter(ClientRequestContext request) throws IOException {
        ChecksumAdapter adapter = new ChecksumAdapter(request.getAdapter());

        Boolean verifyWrite = (Boolean) request.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
        if (verifyWrite != null && verifyWrite) {
            // wrap stream to calculate write checksum
            request.setAdapter(adapter);
        }

        Boolean generateMd5 = (Boolean) request.getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5);
        if (generateMd5 != null && generateMd5) {
            // wrap stream to generate Content-MD5 header
            ContentMd5Adapter md5Adapter = new ContentMd5Adapter(request.getAdapter());
            request.setAdapter(md5Adapter);
        }
    }

    @Override
    public void filter(ClientRequestContext request, ClientResponseContext response) throws IOException {

        ChecksumAdapter adapter = new ChecksumAdapter(request.getAdapter());
        Boolean verifyWrite = (Boolean) request.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);

        // pull etag from response headers
        String md5Header = RestUtil.getFirstAsString(response.getHeaders(), RestUtil.HEADER_ETAG);
        if (md5Header != null) md5Header = md5Header.replaceAll("\"", "");
        if (md5Header != null && (md5Header.length() <= 2 || md5Header.contains("-")))
            md5Header = null; // look for valid etags

        // also look for content MD5 (this trumps etag if present)
        String contentMd5 = RestUtil.getFirstAsString(response.getHeaders(), RestUtil.EMC_CONTENT_MD5);
        if (contentMd5 != null) md5Header = contentMd5;

        if (verifyWrite != null && verifyWrite && md5Header != null) {
            // verify write checksum
            if (!adapter.getChecksum().getHexValue().equals(md5Header))
                throw new ChecksumError("Checksum failure while writing stream", adapter.getChecksum().getHexValue(), md5Header);
        }

        Boolean verifyRead = (Boolean) request.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM);
        if (verifyRead != null && verifyRead && md5Header != null) {
            // wrap stream to verify read checksum
            request.setEntityStream(new ChecksummedInputStream(request.getEntityStream(),
                    new ChecksumValueImpl(ChecksumAlgorithm.MD5, 0, md5Header))); // won't have length for chunked responses
        }

//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException("fatal: MD5 algorithm not found");
//        }
    }

    private class ChecksumAdapter extends AbstractClientRequestAdapter {
        RunningChecksum checksum;

        ChecksumAdapter(ClientRequestAdapter parent) {
            super(parent);
        }

        @Override
        public OutputStream adapt(ClientRequest request, OutputStream out) throws IOException {
            try {
                checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                out = new ChecksummedOutputStream(out, checksum);
                return getAdapter().adapt(request, out); // don't break the chain
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }

        public RunningChecksum getChecksum() {
            return checksum;
        }
    }

    private class ContentMd5Adapter extends AbstractClientRequestAdapter implements CloseEventListener {
        ClientRequest request;
        OutputStream finalStream;
        RunningChecksum checksum;
        ByteArrayOutputStream buffer;

        ContentMd5Adapter(ClientRequestAdapter parent) {
            super(parent);
        }

        @Override
        public OutputStream adapt(ClientRequest request, OutputStream out) throws IOException {
            this.request = request;
            finalStream = out;
            try {
                checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                buffer = new ByteArrayOutputStream();
                out = new CloseNotifyOutputStream(buffer, this);
                out = new ChecksummedOutputStream(out, checksum);
                return getAdapter().adapt(request, out); // don't break the chain
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }

        @Override
        public void streamClosed(CloseNotifyOutputStream stream) throws IOException {
            // add Content-MD5 (before anything is written to the final stream)
            request.getHeaders().putSingle(RestUtil.HEADER_CONTENT_MD5,
                    DatatypeConverter.printBase64Binary(checksum.getByteValue()));

            // need to re-sign request because Content-MD5 is included in the signature!
            if (s3Config.getIdentity() != null) {
                Map<String, String> parameters = RestUtil.getQueryParameterMap(request.getUri().getRawQuery());

                String resource = VHostUtil.getResourceString(s3Config,
                        (String) request.getProperty(RestUtil.PROPERTY_NAMESPACE),
                        (String) request.getProperty(S3Constants.PROPERTY_BUCKET_NAME),
                        RestUtil.getEncodedPath(request.getUri()));

                signer.sign(request,
                        resource,
                        parameters,
                        request.getHeaders());
            }

            // write the complete buffered data
            finalStream.write(buffer.toByteArray());
        }
    }

    private class CloseNotifyOutputStream extends FilterOutputStream {
        private List<CloseEventListener> listeners = new ArrayList<CloseEventListener>();

        CloseNotifyOutputStream(OutputStream out, CloseEventListener... listeners) {
            super(out);
            if (listeners != null) this.listeners.addAll(Arrays.asList(listeners));
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            super.close();
            for (CloseEventListener listener : listeners) {
                listener.streamClosed(this);
            }
        }
    }

    private interface CloseEventListener extends EventListener {
        void streamClosed(CloseNotifyOutputStream stream) throws IOException;
    }
}
