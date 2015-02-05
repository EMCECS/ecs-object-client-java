/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.jersey;

import com.emc.object.EncryptionConfig;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.ObjectReceiver;
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

public class EncryptionFilter extends ClientFilter {
    EncryptionTransformFactory factory;

    public EncryptionFilter(EncryptionTransformFactory factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        ObjectReceiver receiver = (ObjectReceiver) request.getProperties().get(RestUtil.PROPERTY_ENCRYPTION_OUTPUT_TRANSFORM_RECEIVER);
        if (receiver != null) {
            request.setAdapter(new EncryptAdapter(request.getAdapter(), receiver));
        }

        ClientResponse response = getNext().handle(request);

        try {
            receiver = (ObjectReceiver) request.getProperties().get(RestUtil.PROPERTY_ENCRYPTION_INPUT_TRANSFORM_RECEIVER);
            if (receiver != null) {
                Map<String, String> userMetadata = S3ObjectMetadata.getUserMetadata(response.getHeaders());
                String encryptionMode = EncryptionConfig.getEncryptionMode(userMetadata);
                InputTransform transform = factory.getInputTransform(encryptionMode, response.getEntityInputStream(), userMetadata);
                receiver.setObject(transform);
                response.setEntityInputStream(transform.getDecodedInputStream());
            }
        } catch (IOException | TransformException e) {
            throw new ClientHandlerException("error decrypting object content", e);
        }

        return response;
    }

    private class EncryptAdapter extends AbstractClientRequestAdapter {
        ObjectReceiver<OutputTransform> receiver;

        EncryptAdapter(ClientRequestAdapter parent, ObjectReceiver<OutputTransform> receiver) {
            super(parent);
            this.receiver = receiver;
        }

        @Override
        public OutputStream adapt(ClientRequest request, OutputStream out) throws IOException {
            try {
                OutputTransform transform = factory.getOutputTransform(out, new HashMap<String, String>());
                receiver.setObject(transform);
                return transform.getEncodedOutputStream();
            } catch (TransformException e) {
                throw new ClientHandlerException("error encrypting object content", e);
            }
        }
    }
}
