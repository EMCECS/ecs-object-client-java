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

import com.emc.object.EncryptionConfig;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.CloseEventOutputStream;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.SizeOverrideWriter;
import com.emc.rest.util.SizedInputStream;
import com.emc.vipr.transform.InputTransform;
import com.emc.vipr.transform.OutputTransform;
import com.emc.vipr.transform.TransformException;
import com.emc.vipr.transform.encryption.EncryptionTransformFactory;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class CodecFilter extends ClientFilter {
    EncryptionTransformFactory factory;

    public CodecFilter(EncryptionTransformFactory factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        Map<String, String> userMeta = (Map<String, String>) request.getProperties().get(RestUtil.PROPERTY_USER_METADATA);

        Boolean encode = (Boolean) request.getProperties().get(RestUtil.PROPERTY_ENCODE_ENTITY);
        if (encode != null && encode) {

            // alter entity size (if necessary) to reflect encrypted size
            Long contentLength = SizeOverrideWriter.getEntitySize();
            if (contentLength != null && contentLength >= 0)
                SizeOverrideWriter.setEntitySize((contentLength / 16 + 1) * 16);

            // wrap output stream with encryptor
            request.setAdapter(new EncryptAdapter(request.getAdapter(), userMeta));
        }

        // execute request
        ClientResponse response = getNext().handle(request);

        try {
            Boolean decode = (Boolean) request.getProperties().get(RestUtil.PROPERTY_DECODE_ENTITY);
            if (decode != null && decode) {

                // get encryption spec from metadata
                Map<String, String> userMetadata = S3ObjectMetadata.getUserMetadata(response.getHeaders());
                String encryptionMode = EncryptionConfig.getEncryptionMode(userMetadata);

                // wrap input stream with decryptor
                InputTransform transform = factory.getInputTransform(encryptionMode, response.getEntityInputStream(), userMetadata);
                response.setEntityInputStream(transform.getDecodedInputStream());
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new ClientHandlerException("error decrypting object content", e);
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
            try {
                final OutputTransform transform = factory.getOutputTransform(out, new HashMap<String, String>());
                out = new CloseEventOutputStream(transform.getEncodedOutputStream(), new Runnable() {
                    @Override
                    public void run() {
                        encMeta.clear();
                        encMeta.putAll(transform.getEncodedMetadata());
                        EncryptionConfig.setEncryptionMode(encMeta, transform.getTransformConfig());
                    }
                });
                return getAdapter().adapt(request, out); // don't break the chain
            } catch (TransformException e) {
                throw new ClientHandlerException("error encrypting object content", e);
            }
        }
    }
}
