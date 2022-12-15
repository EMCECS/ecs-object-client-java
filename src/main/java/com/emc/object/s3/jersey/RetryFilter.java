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
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Exception;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class RetryFilter implements ClientRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RetryFilter.class);

    public static final String PROP_RETRY_COUNT = "com.emc.object.retryCount";

    private S3Config s3Config;

    public RetryFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        int retryCount = 0;
        InputStream entityStream = null;
        if (request.getEntity() instanceof InputStream) entityStream = (InputStream) request.getEntity();
        while (true) {
            try {
                // if using an InputStream, mark the stream so we can rewind it in case of an error
                if (entityStream != null && entityStream.markSupported())
                    entityStream.mark(s3Config.getRetryBufferSize());

//                return getNext().handle(clientRequest);
            } catch (RuntimeException orig) {
                Throwable t = orig;

                // in this case, the exception was wrapped by Jersey
                if (t instanceof IOException) t = t.getCause();

                if (t instanceof S3Exception) {
                    S3Exception se = (S3Exception) t;

                    // retry all 50x errors except 501 (not implemented)
                    if (se.getHttpCode() < 500 || se.getHttpCode() == 501) throw orig;

                    // retry all IO exceptions
                } else if (!(t instanceof IOException)) throw orig;

                // only retry retryLimit times
                if (++retryCount > s3Config.getRetryLimit()) throw orig;

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

                // wait for retry delay
                if (s3Config.getInitialRetryDelay() > 0) {
                    int retryDelay = s3Config.getInitialRetryDelay() * (int) Math.pow(2, retryCount - 1);
                    try {
                        log.debug("waiting {}ms before retry", retryDelay);
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        log.warn("interrupted while waiting to retry: " + e.getMessage());
                    }
                }

                log.info("error received in response [{}], retrying ({} of {})...", t, retryCount, s3Config.getRetryLimit());
                request.getConfiguration().getProperties().put(PROP_RETRY_COUNT, retryCount);
            }
        }
    }
}
