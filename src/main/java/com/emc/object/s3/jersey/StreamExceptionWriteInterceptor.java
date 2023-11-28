package com.emc.object.s3.jersey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

@Provider
public class StreamExceptionWriteInterceptor implements WriterInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StreamExceptionWriteInterceptor.class);

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.proceed();
    }

}