package com.emc.object.s3.jersey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;

@Provider
public class StreamExceptionReadInterceptor implements ReaderInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StreamExceptionReadInterceptor.class);

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        return context.proceed();
    }
}
