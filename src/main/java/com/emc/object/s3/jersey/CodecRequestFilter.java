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

import com.emc.codec.CodecChain;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.SizeOverrideWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Provider
@Priority(FilterPriorities.PRIORITY_CODEC_REQUEST)
public class CodecRequestFilter implements ClientRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CodecRequestFilter.class);

    private final CodecChain encodeChain;
    private Map<String, Object> codecProperties;

    public CodecRequestFilter(CodecChain encodeChain) {
        this.encodeChain = encodeChain;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        Map<String, String> userMeta = (Map<String, String>) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_USER_METADATA);

        Boolean encode = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_ENCODE_ENTITY);
        if (encode != null && encode) {

            // if encoded size is predictable and we know the original size, we can set a content-length and avoid chunked encoding
            Long originalSize = SizeOverrideWriter.getEntitySize();
            if (encodeChain.isSizePredictable() && originalSize != null) {
                long encodedSize = encodeChain.getEncodedSize(originalSize);
                log.debug("updating content-length for encoded data (original: {}, encoded: {})", originalSize, encodedSize);
                SizeOverrideWriter.setEntitySize(encodedSize);
            } else {
                // we don't know what the size will be; this will turn on chunked encoding in the apache client
                SizeOverrideWriter.setEntitySize(-1L);
            }

            // backup original metadata in case of an error
            requestContext.setProperty(RestUtil.PROPERTY_META_BACKUP, new HashMap<String, String>(userMeta));

            OutputStream encodeStream = encodeChain.getEncodeStream(requestContext.getEntityStream(), userMeta);
            requestContext.getHeaders().putAll(S3ObjectMetadata.getUmdHeaders(userMeta));
            requestContext.setEntityStream(encodeStream);
        }
    }

    public Map<String, Object> getCodecProperties() {
        return codecProperties;
    }

    public void setCodecProperties(Map<String, Object> codecProperties) {
        this.codecProperties = codecProperties;
    }

    public CodecRequestFilter withCodecProperties(Map<String, Object> codecProperties) {
        setCodecProperties(codecProperties);
        return this;
    }

}