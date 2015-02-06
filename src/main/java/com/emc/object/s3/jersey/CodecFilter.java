/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.jersey;

import com.emc.object.EncryptionConfig;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.CloseEventOutputStream;
import com.emc.object.util.RestUtil;
import com.emc.vipr.transform.InputTransform;
import com.emc.vipr.transform.OutputTransform;
import com.emc.vipr.transform.TransformException;
import com.emc.vipr.transform.encryption.EncryptionTransformFactory;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;

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
        Map<String, String> encMeta = (Map<String, String>) request.getProperties().get(RestUtil.PROPERTY_ENCODE_METADATA);
        if (encMeta != null) {
            request.setAdapter(new EncryptAdapter(request.getAdapter(), encMeta));
        }

        ClientResponse response = getNext().handle(request);

        try {
            Boolean decode = (Boolean) request.getProperties().get(RestUtil.PROPERTY_DECODE_METADATA);
            if (decode != null && decode) {
                Map<String, String> userMetadata = S3ObjectMetadata.getUserMetadata(response.getHeaders());
                String encryptionMode = EncryptionConfig.getEncryptionMode(userMetadata);
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
                out = getAdapter().adapt(request, out); // don't break the chain
                final OutputTransform transform = factory.getOutputTransform(out, new HashMap<String, String>());
                return new CloseEventOutputStream(transform.getEncodedOutputStream(), new Runnable() {
                    @Override
                    public void run() {
                        encMeta.clear();
                        encMeta.putAll(transform.getEncodedMetadata());
                        EncryptionConfig.setEncryptionMode(encMeta, transform.getTransformConfig());
                    }
                });
            } catch (TransformException e) {
                throw new ClientHandlerException("error encrypting object content", e);
            }
        }
    }
}
