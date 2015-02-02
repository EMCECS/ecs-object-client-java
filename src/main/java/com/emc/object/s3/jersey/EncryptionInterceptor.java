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

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EncryptionInterceptor implements WriterInterceptor, ReaderInterceptor {
    EncryptionTransformFactory factory;

    public EncryptionInterceptor(EncryptionTransformFactory factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        try {
            ObjectReceiver receiver = (ObjectReceiver) context.getProperty(RestUtil.PROPERTY_ENCRYPTION_OUTPUT_TRANSFORM_RECEIVER);
            if (receiver != null) {
                OutputTransform transform = factory.getOutputTransform(context.getOutputStream(), new HashMap<String, String>());
                receiver.setObject(transform);
                context.setOutputStream(transform.getEncodedOutputStream());
            }
        } catch (TransformException e) {
            throw new ProcessingException("Error encrypting object content", e);
        }
        context.proceed();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        try {
            ObjectReceiver receiver = (ObjectReceiver) context.getProperty(RestUtil.PROPERTY_ENCRYPTION_INPUT_TRANSFORM_RECEIVER);
            if (receiver != null) {
                Map<String, String> userMetadata = S3ObjectMetadata.getUserMetadata(context.getHeaders());
                String encryptionMode = EncryptionConfig.getEncryptionMode(userMetadata);
                InputTransform transform = factory.getInputTransform(encryptionMode, context.getInputStream(), userMetadata);
                receiver.setObject(transform);
                context.setInputStream(transform.getDecodedInputStream());
            }
        } catch (TransformException e) {
            throw new ProcessingException("Error decrypting object content", e);
        }
        return context.proceed();
    }
}
