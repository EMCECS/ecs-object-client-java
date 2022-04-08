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
package com.emc.object.s3.jersey;

import com.emc.codec.CodecChain;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.SizeOverrideWriter;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CodecFilter extends ClientFilter {

    private static final Logger log = LoggerFactory.getLogger(CodecFilter.class);

    private CodecChain encodeChain;
    private Map<String, Object> codecProperties;

    public CodecFilter(CodecChain encodeChain) {
        this.encodeChain = encodeChain;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        Map<String, String> userMeta = (Map<String, String>) request.getProperties().get(RestUtil.PROPERTY_USER_METADATA);
        Map<String, String> metaBackup = null;

        Boolean encode = (Boolean) request.getProperties().get(RestUtil.PROPERTY_ENCODE_ENTITY);
        if (encode != null && encode) {

            // if encoded size is predictable and we know the original size, we can set a content-length and avoid chunked encoding
            Long originalSize = SizeOverrideWriter.getEntitySize();
            if (encodeChain.isSizePredictable() && originalSize != null) {
                long encodedSize = encodeChain.getEncodedSize(originalSize);
                log.debug("updating content-length for encoded data (original: {}, encoded: {})", originalSize, encodedSize);
                SizeOverrideWriter.setEntitySize(encodedSize);
            } else {
                // we don't know what the size will be; this will turn on chunked encoding in the apache client
                SizeOverrideWriter.setEntitySize(-1L);
            }

            // backup original metadata in case of an error
            metaBackup = new HashMap<String, String>(userMeta);

            // we need pre-stream metadata from the encoder, but we don't have the entity output stream, so we'll use
            // a "dangling" output stream and connect it in the adapter
            // NOTE: we can't alter the headers in the adapt() method because they've already been a) signed and b) sent
            DanglingOutputStream danglingStream = new DanglingOutputStream();
            OutputStream encodeStream = encodeChain.getEncodeStream(danglingStream, userMeta);

            // add pre-stream encode metadata
            request.getHeaders().putAll(S3ObjectMetadata.getUmdHeaders(userMeta));

            // wrap output stream with encryptor
            request.setAdapter(new EncryptAdapter(request.getAdapter(), danglingStream, encodeStream));
        }

        // execute request
        ClientResponse response;
        try {
            response = getNext().handle(request);
        } catch (RuntimeException e) {
            if (encode != null && encode) {
                // restore metadata from backup
                userMeta.clear();
                userMeta.putAll(metaBackup);
            }
            throw e;
        }

        // get user metadata from response headers
        MultivaluedMap<String, String> headers = response.getHeaders();
        Map<String, String> storedMeta = S3ObjectMetadata.getUserMetadata(headers);
        Set<String> keysToRemove = new HashSet<String>();
        keysToRemove.addAll(storedMeta.keySet());

        // get encode specs from user metadata
        String[] encodeSpecs = CodecChain.getEncodeSpecs(storedMeta);
        if (encodeSpecs != null) {

            // create codec chain
            CodecChain decodeChain = new CodecChain(encodeSpecs).withProperties(codecProperties);

            // do we need to decode the entity?
            Boolean decode = (Boolean) request.getProperties().get(RestUtil.PROPERTY_DECODE_ENTITY);
            if (decode != null && decode) {

                // wrap input stream with decryptor (this will remove any encode metadata from storedMeta)
                response.setEntityInputStream(decodeChain.getDecodeStream(response.getEntityInputStream(), storedMeta));
            } else {

                // need to remove any encode metadata so we can update the headers
                decodeChain.removeEncodeMetadata(storedMeta, decodeChain.getEncodeMetadataList(storedMeta));
            }

            // should we keep the encode headers?
            Boolean keepHeaders = (Boolean) request.getProperties().get(RestUtil.PROPERTY_KEEP_ENCODE_HEADERS);
            if (keepHeaders == null || !keepHeaders) {

                // remove encode metadata from headers (storedMeta now contains only user-defined metadata)
                keysToRemove.removeAll(storedMeta.keySet()); // all metadata - user-defined metadata
                for (String key : keysToRemove) {
                    headers.remove(S3ObjectMetadata.getHeaderName(key));
                }
            }
        }

        return response;
    }

    // only way to set the output stream
    private class EncryptAdapter extends AbstractClientRequestAdapter {
        DanglingOutputStream danglingStream;
        OutputStream encodeStream;

        EncryptAdapter(ClientRequestAdapter parent, DanglingOutputStream danglingStream, OutputStream encodeStream) {
            super(parent);
            this.danglingStream = danglingStream;
            this.encodeStream = encodeStream;
        }

        @Override
        public OutputStream adapt(ClientRequest request, OutputStream out) throws IOException {
            danglingStream.setOutputStream(out); // connect the dangling output stream
            return getAdapter().adapt(request, encodeStream); // don't break the chain
        }
    }

    public Map<String, Object> getCodecProperties() {
        return codecProperties;
    }

    public void setCodecProperties(Map<String, Object> codecProperties) {
        this.codecProperties = codecProperties;
    }

    public CodecFilter withCodecProperties(Map<String, Object> codecProperties) {
        setCodecProperties(codecProperties);
        return this;
    }

    private static class DanglingOutputStream extends FilterOutputStream {
        private static final OutputStream BOGUS_STREAM = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new RuntimeException("you didn't connect a dangling output stream!");
            }
        };

        DanglingOutputStream() {
            super(BOGUS_STREAM);
        }

        void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException("single-byte write called!");
        }
    }
}
