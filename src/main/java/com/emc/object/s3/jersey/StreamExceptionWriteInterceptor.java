package com.emc.object.s3.jersey;

import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.SizeOverrideWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Map;

@Provider
public class StreamExceptionWriteInterceptor implements WriterInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StreamExceptionWriteInterceptor.class);

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        try {
            context.proceed();
        } catch (Exception e) {
            Boolean encode = context.getProperty(RestUtil.PROPERTY_META_BACKUP) != null;
            Map<String, String> userMeta = (Map<String, String>) context.getProperty(RestUtil.PROPERTY_USER_METADATA);

            if (encode != null && encode) {
                // restore metadata from backup
                userMeta.clear();
                userMeta.putAll((Map<String, String>) context.getProperty(RestUtil.PROPERTY_META_BACKUP));
            }
            SizeOverrideWriter.setEntitySize(null);
            throw e;
        }
    }

}