package com.emc.object.s3.jersey;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Priority;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(FilterPriorities.PRIORITY_RETRY)
public class RetryFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final int MAX_RETRIES = 3;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        // nothing to do before the request is sent
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        int retries = 0;
        while (responseContext.getStatus() >= 500 || isIOException(responseContext.getHeaders().getFirst("Exception")) && retries < MAX_RETRIES) {
            retries++;
            closeResponse(responseContext);
            try {
                Thread.sleep(1000 * retries);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProcessingException("Retry interrupted", e);
            }
            Response retryResponse = requestContext
                    .getClient()
                    .target(requestContext.getUri())
                    .request(requestContext.getMethod())
                    .headers(requestContext.getHeaders())
                    .property("jersey.config.client.connectTimeout", requestContext.getConfiguration().getProperty("jersey.config.client.connectTimeout"))
                    .property("jersey.config.client.readTimeout", requestContext.getConfiguration().getProperty("jersey.config.client.readTimeout"))
//                    .property("jersey.config.client.followRedirects", requestContext.getConfiguration().getProperty("jersey.config.client.followRedirects"))
                    .method(requestContext.getMethod(), (Entity<?>) requestContext.getEntity());
            responseContext.setStatusInfo(retryResponse.getStatusInfo());
            responseContext.setStatus(retryResponse.getStatus());
            responseContext.setEntityStream(retryResponse.readEntity(InputStream.class));
//            responseContext.getHeaders().putAll(retryResponse.getHeaders());
            retryResponse.close();
        }
    }

    private boolean isIOException(Object exceptionHeader) {
        return exceptionHeader instanceof IOException;
    }

    private void closeResponse(ClientResponseContext responseContext) {
        if (responseContext.getEntityStream() != null) {
            try {
                responseContext.getEntityStream().close();
            } catch (IOException e) {
                // ignore exception
            }
        }
    }
}

