/*
 * Copyright (c) 2015-2016, EMC Corporation.
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
package com.emc.object;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.object.s3.S3Exception;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.SizeOverrideWriter;

public abstract class AbstractJerseyClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractJerseyClient.class);

    public static final String PROP_RETRY_COUNT = "com.emc.object.retryCount";

    protected ObjectConfig objectConfig;

    protected AbstractJerseyClient(ObjectConfig objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected Response executeAndClose(Client client, ObjectRequest request) {
        Response response = executeRequest(client, request);
        response.close();
        return response;
    }

    /**
     * Override in subclasses that support retry. Default is {@code false} (no retry).
     */
    protected boolean isRetryEnabled() {
        return false;
    }

    /**
     * Override in subclasses to provide the maximum number of retries.
     */
    protected int getRetryLimit() {
        return 0;
    }

    /**
     * Override in subclasses to provide the initial retry delay in milliseconds.
     * Exponential backoff is applied: delay = initialRetryDelay * 2^(retryCount-1).
     */
    protected int getInitialRetryDelay() {
        return 0;
    }

    /**
     * Override in subclasses to provide the buffer size for marking input streams during retry.
     */
    protected int getRetryBufferSize() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    protected Response executeRequest(Client client, ObjectRequest request) {
        boolean retryEnabled = isRetryEnabled();
        InputStream entityStream = null;
        if (retryEnabled && request instanceof EntityRequest) {
            Object entity = ((EntityRequest) request).getEntity();
            if (entity instanceof InputStream) {
                int bufSize = getRetryBufferSize();
                InputStream is = (InputStream) entity;
                InputStream buffered = is.markSupported() ? is : new BufferedInputStream(is, bufSize);
                buffered.mark(bufSize);
                entityStream = buffered;
            }
        }
        int retryCount = 0;
        while (true) {
            try {
                return unwrapAndExecute(client, request);
            } catch (RuntimeException orig) {
                if (!retryEnabled) throw orig;
                retryCount++;
                // stash retry count so GeoPinningFilter can fail over on reads
                request.property(PROP_RETRY_COUNT, retryCount);
                // checkRetry throws the original exception if retries are exhausted or not retryable
                checkRetry(orig, retryCount, entityStream);
            }
        }
    }

    /**
     * Checks whether the given exception is retryable and handles retry logic including
     * exponential backoff. Throws the original exception if retries are exhausted or the
     * error is not retryable.
     */
    private void checkRetry(RuntimeException orig, int retryCount, InputStream entityStream) {
        Throwable t = orig;

        // in this case, the exception was wrapped by Jersey
        if (t instanceof ProcessingException) t = t.getCause();

        if (t instanceof S3Exception) {
            S3Exception se = (S3Exception) t;

            // retry all 50x errors except 501 (not implemented)
            if (se.getHttpCode() < 500 || se.getHttpCode() == 501) throw orig;

            // retry all IO exceptions
        } else if (!(t instanceof IOException)) throw orig;

        // only retry retryLimit times
        if (retryCount > getRetryLimit()) throw orig;

        // attempt to reset InputStream
        if (entityStream != null) {
            try {
                if (!entityStream.markSupported()) throw new IOException("stream does not support mark/reset");
                entityStream.reset();
            } catch (IOException e) {
                log.warn("could not reset entity stream for retry: " + e);
                throw orig;
            }
        }

        // wait for retry delay with exponential backoff
        int initialDelay = getInitialRetryDelay();
        if (initialDelay > 0) {
            int retryDelay = initialDelay * (int) Math.pow(2, retryCount - 1);
            try {
                log.debug("waiting {}ms before retry", retryDelay);
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                log.warn("interrupted while waiting to retry: " + e.getMessage());
            }
        }

        log.info("error received in response [{}], retrying ({} of {})...", new Object[] { t, retryCount, getRetryLimit() });
    }

    private Response unwrapAndExecute(Client client, ObjectRequest request) {
        try {
            return doExecuteRequest(client, request);
        } catch (javax.ws.rs.ProcessingException e) {
            // Jersey 2 wraps any runtime exception (from response filters AND from entity writers /
            // connector I/O) in a ProcessingException. Only unwrap when the exception was thrown
            // from ErrorFilter (i.e. a parsed server-side S3 error); let other wrappings surface
            // as ProcessingException so callers can distinguish transport/stream failures.
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException && isFromErrorFilter(cause)) {
                throw (RuntimeException) cause;
            }
            throw e;
        }
    }

    private static boolean isFromErrorFilter(Throwable cause) {
        // Only unwrap S3Exceptions raised directly from filters whose purpose is to translate
        // server responses (ErrorFilter) or synthetic client-side rejections (FaultInjectionFilter).
        // Stream/IO failures from the entity-write path intentionally remain wrapped in a
        // ProcessingException so callers can distinguish transport-level failures.
        // Note: WriterInterceptors like ChecksumFilter must NOT be matched here, since exceptions
        // thrown while reading a user-supplied InputStream pass through their frames.
        for (StackTraceElement frame : cause.getStackTrace()) {
            String cn = frame.getClassName();
            if ("com.emc.object.s3.jersey.ErrorFilter".equals(cn)) return true;
            if ("com.emc.object.s3.jersey.FaultInjectionFilter".equals(cn)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Response doExecuteRequest(Client client, ObjectRequest request) {
        try {
            if (request.getMethod().isRequiresEntity()) {
                String contentType = RestUtil.DEFAULT_CONTENT_TYPE;
                Object entity = new byte[0];
                if (request instanceof EntityRequest) {
                    EntityRequest entityRequest = (EntityRequest) request;

                    if (entityRequest.getContentType() != null) contentType = entityRequest.getContentType();

                    if (entityRequest.getEntity() != null) entity = entityRequest.getEntity();

                    // if content-length is set (perhaps by user), force jersey to use it
                    if (entityRequest.getContentLength() != null) {
                        log.debug("enabling content-length override ({})", entityRequest.getContentLength().toString());
                        SizeOverrideWriter.setEntitySize(entityRequest.getContentLength());

                        // otherwise chunked encoding will be used. if the request does not support it, try to ensure
                        // that the entity is buffered (will set content length from buffered write)
                    } else if (!entityRequest.isChunkable()) {
                        log.debug("no content-length and request is not chunkable, attempting to enable buffering");
                        request.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);
                        request.property(ClientProperties.CHUNKED_ENCODING_SIZE, null);
                    }
                } else {

                    // no entity, but make sure the apache handler doesn't mess up the content-length somehow
                    // (i.e. if content-encoding is set)
                    request.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);

                    String headerContentType = RestUtil.getFirstAsString(request.getHeaders(), RestUtil.HEADER_CONTENT_TYPE);
                    if (headerContentType != null) contentType = headerContentType;
                }

                Invocation.Builder builder = buildRequest(client, request);

                // jersey requires content-type for entity requests
                // NOTE: Jersey 2's JerseyInvocation.storeEntity(Entity) calls request.variant(entity.getVariant()),
                // which in turn REMOVES the Content-Encoding header when the variant encoding is null.
                // Preserve any Content-Encoding header by pushing it into the Entity's Variant.
                String contentEncoding = RestUtil.getFirstAsString(request.getHeaders(), RestUtil.HEADER_CONTENT_ENCODING);
                javax.ws.rs.core.Variant variant = new javax.ws.rs.core.Variant(
                        javax.ws.rs.core.MediaType.valueOf(contentType), (String) null, contentEncoding);
                return builder.method(request.getMethod().toString(), Entity.entity(entity, variant));
            } else { // non-entity request method

                // can't send content with non-entity methods (GET, HEAD, etc.)
                if (request instanceof EntityRequest)
                    throw new UnsupportedOperationException("an entity request is using a non-entity method (" + request.getMethod() + ")");

                Invocation.Builder builder = buildRequest(client, request);

                return builder.method(request.getMethod().toString());
            }
        } finally {
            // make sure we clear the content-length override for this thread
            SizeOverrideWriter.setEntitySize(null);
        }
    }

    protected <T> T executeRequest(Client client, ObjectRequest request, Class<T> responseType) {
        Response response = executeRequest(client, request);
        T responseEntity = response.readEntity(responseType);
        fillResponseEntity(responseEntity, response);
        return responseEntity;
    }

    protected void fillResponseEntity(Object responseEntity, Response response) {
        if (responseEntity instanceof ObjectResponse)
            ((ObjectResponse) responseEntity).setHeaders(response.getStringHeaders());
    }

    protected Invocation.Builder buildRequest(Client client, ObjectRequest request) {
        URI uri = objectConfig.resolvePath(request.getPath(), request.getRawQueryString());
        WebTarget target = client.target(uri);

        Invocation.Builder builder = target.request();

        // set per-request properties on the Invocation.Builder so that they are visible
        // to ClientRequestFilter/ClientResponseFilter via ClientRequestContext#getProperty().
        // Properties set on WebTarget live in its Configuration and are not exposed via
        // ClientRequestContext#getProperty().
        for (Map.Entry<String, Object> entry : request.getProperties().entrySet()) {
            builder = builder.property(entry.getKey(), entry.getValue());
        }

        // set namespace
        String namespace = request.getNamespace() != null ? request.getNamespace() : objectConfig.getNamespace();
        if (namespace != null)
            builder = builder.property(RestUtil.PROPERTY_NAMESPACE, namespace);

        // set headers
        for (String name : request.getHeaders().keySet()) {
            for (Object value : request.getHeaders().get(name)) {
                builder = builder.header(name, value);
            }
        }

        return builder;
    }

    public ObjectConfig getObjectConfig() {
        return objectConfig;
    }
}
