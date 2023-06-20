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

import com.emc.object.s3.S3Exception;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.SizeOverrideWriter;
import org.glassfish.jersey.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static com.emc.object.ObjectConfig.PROPERTY_RETRY_COUNT;

public abstract class AbstractJerseyClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractJerseyClient.class);

    protected ObjectConfig objectConfig;

    protected AbstractJerseyClient(ObjectConfig objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected Response executeAndClose(JerseyClient client, ObjectRequest request) {
        return executeRequest(client, request);
    }

    @SuppressWarnings("unchecked")
    protected Response executeRequest(JerseyClient client, ObjectRequest request) {
        String contentType = null;
        //TODO: should enable contentEncoding feature to EntityRequest. Make a simple workaround fix for test case here.
        String contentEncoding = RestUtil.getFirstAsString(request.getHeaders(), RestUtil.HEADER_CONTENT_ENCODING);
        Object entity = null;
        Method method = request.getMethod();
        if (method.isRequiresEntity()) {
            contentType = RestUtil.DEFAULT_CONTENT_TYPE;
            entity = new byte[0];

            if (request instanceof EntityRequest) {
                EntityRequest entityRequest = (EntityRequest) request;

                if (entityRequest.getContentType() != null) contentType = entityRequest.getContentType();

                if (entityRequest.getEntity() != null)
                    entity = entityRequest.getEntity();

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

        } else { // non-entity request method

            // can't send content with non-entity methods (GET, HEAD, etc.)
            if (request instanceof EntityRequest)
                throw new UnsupportedOperationException("an entity request is using a non-entity method (" + request.getMethod() + ")");

        }

        JerseyInvocation.Builder builder = buildRequest(client, request);

        // retry
        int retryCount = 0;
        InputStream entityStream = null;
        if (entity instanceof InputStream)
            entityStream = (InputStream) entity;

        while (true) {
            try {
                // if using an InputStream, mark the stream so we can rewind it in case of an error
                if (objectConfig.isRetryEnabled() && entityStream != null && entityStream.markSupported())
                    entityStream.mark(objectConfig.getRetryBufferSize());

                return Objects.isNull(entity)
                        ? builder.method(method.name())
                        : builder.method(method.name(), Entity.entity(entity, new Variant(MediaType.valueOf(contentType), (String) null, contentEncoding)));

            } catch (RuntimeException orig) {
                Throwable t = orig;

                // in this case, the exception was wrapped by Jersey
                if (t instanceof ProcessingException)
                    t = t.getCause();

                if (t instanceof S3Exception) {
                    S3Exception se = (S3Exception) t;

                    // retry all 50x errors except 501 (not implemented)
                    if (se.getHttpCode() < 500 || se.getHttpCode() == 501)
                        throw se;

                    // retry all IO exceptions
                } else if (!(t instanceof IOException))
                    throw orig;

                // clean usermetadata in PutObject encryption requests
                Boolean encode = (Boolean) request.getProperties().get(RestUtil.PROPERTY_ENCODE_ENTITY);
                if (encode != null && encode) {
                    Map<String, String> userMeta = (Map<String, String>) request.getProperties().get(RestUtil.PROPERTY_USER_METADATA);
                    userMeta.clear();
                }

                if (!objectConfig.isRetryEnabled())
                    throw orig;

                // only retry retryLimit times
                if (++retryCount > objectConfig.getRetryLimit()) throw orig;

                // attempt to reset InputStream
                if (entityStream != null) {
                    try {
                        entityStream.reset();
                    } catch (IOException e) {
                        log.warn("could not reset entity stream for retry: " + e);
                        throw orig;
                    }
                }

                // wait for retry delay
                if (objectConfig.getInitialRetryDelay() > 0) {
                    int retryDelay = objectConfig.getInitialRetryDelay() * (int) Math.pow(2, retryCount - 1);
                    try {
                        log.debug("waiting {}ms before retry", retryDelay);
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        log.warn("interrupted while waiting to retry: " + e.getMessage());
                    }
                    log.warn("error received in response [{}], retrying ({} of {})...", t, retryCount, objectConfig.getRetryLimit());
                    request.property(PROPERTY_RETRY_COUNT, retryCount);
                }
            } finally {
                // make sure we clear the content-length override for this thread
                SizeOverrideWriter.setEntitySize(null);
            }
        }
    }

    protected <T> T executeRequest(JerseyClient client, ObjectRequest request, Class<T> responseType) {
        Response response = executeRequest(client, request);
        T responseEntity = response.readEntity(responseType);
        fillResponseEntity(responseEntity, response);
        response.close(); // in Jersey 2.x, we should always release resources actively.
        return responseEntity;
    }

    protected void fillResponseEntity(Object responseEntity, Response response) {
        if (responseEntity instanceof ObjectResponse)
            ((ObjectResponse) responseEntity).setHeaders(response.getStringHeaders());
    }

    protected JerseyInvocation.Builder buildRequest(JerseyClient client, ObjectRequest request) {
        URI uri = objectConfig.resolvePath(request.getPath(), request.getRawQueryString());
        JerseyWebTarget webTarget = client.target(uri);

        // set properties
        for (Map.Entry<String, Object> entry : request.getProperties().entrySet()) {
            webTarget.property(entry.getKey(), entry.getValue());
        }

        // set namespace
        String namespace = request.getNamespace() != null ? request.getNamespace() : objectConfig.getNamespace();
        if (namespace != null)
            webTarget.property(RestUtil.PROPERTY_NAMESPACE, namespace);

        JerseyInvocation.Builder builder = webTarget.request();

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
