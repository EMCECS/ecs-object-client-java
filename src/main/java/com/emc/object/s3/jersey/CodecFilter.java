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

import com.emc.codec.CodecChain;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.SizeOverrideWriter;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CodecFilter extends ClientFilter {
    private CodecChain encodeChain;
    private Map<String, Object> codecProperties;

    public CodecFilter(CodecChain encodeChain) {
        this.encodeChain = encodeChain;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        Map<String, String> userMeta = (Map<String, String>) request.getProperties().get(RestUtil.PROPERTY_USER_METADATA);

        Boolean encode = (Boolean) request.getProperties().get(RestUtil.PROPERTY_ENCODE_ENTITY);
        if (encode != null && encode) {

            // if encoded size is predictable and we know the original size, we can set a content-length and avoid chunked encoding
            if (encodeChain.isSizePredictable() && SizeOverrideWriter.getEntitySize() != null) {
                SizeOverrideWriter.setEntitySize(encodeChain.getEncodedSize(SizeOverrideWriter.getEntitySize()));
            } else {
                // we don't know what the size will be; this will turn on chunked encoding in the apache client
                SizeOverrideWriter.setEntitySize(-1L);
            }

            // wrap output stream with encryptor
            request.setAdapter(new EncryptAdapter(request.getAdapter(), userMeta));
        }

        // execute request
        ClientResponse response = getNext().handle(request);

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
        Map<String, String> encMeta;

        EncryptAdapter(ClientRequestAdapter parent, Map<String, String> encMeta) {
            super(parent);
            this.encMeta = encMeta;
        }

        @Override
        public OutputStream adapt(ClientRequest request, OutputStream out) throws IOException {
            return getAdapter().adapt(request, encodeChain.getEncodeStream(out, encMeta)); // don't break the chain
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
}
