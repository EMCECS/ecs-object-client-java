package com.emc.object.s3.jersey;

import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ChecksumInterceptor implements ReaderInterceptor, WriterInterceptor {
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        try {
            boolean verifyChecksum = Boolean.valueOf(context.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM).toString());
            if (verifyChecksum) {
                // pull etag and length from headers and wrap stream with verifier
                S3ObjectMetadata objectMetadata = S3ObjectMetadata.fromHeaders(context.getHeaders());
                ChecksumValue etag = new ChecksumValueImpl(ChecksumAlgorithm.MD5, objectMetadata.getContentLength(), objectMetadata.getETag());
                context.setInputStream(new ChecksummedInputStream(context.getInputStream(), etag));
            }

            return context.proceed();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal: MD5 algorithm not found");
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        RunningChecksum checksum = (RunningChecksum) context.getProperty(RestUtil.PROPERTY_WRITE_CHECKSUM);
        if (checksum != null)
            // wrap stream to calculate checksum, but we can't verify here because we can't see the response headers
            // (this should not be called an "interceptor"!)
            context.setOutputStream(new ChecksummedOutputStream(context.getOutputStream(), checksum));

        context.proceed();
    }
}
