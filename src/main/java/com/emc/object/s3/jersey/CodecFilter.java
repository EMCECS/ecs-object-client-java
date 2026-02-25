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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Provider
public class CodecFilter implements WriterInterceptor, ReaderInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CodecFilter.class);

    private CodecChain encodeChain;
    private Map<String, Object> codecProperties;

    public CodecFilter(CodecChain encodeChain) {
        this.encodeChain = encodeChain;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        Map<String, String> userMeta = (Map<String, String>) context.getProperty(RestUtil.PROPERTY_USER_METADATA);
        Boolean encode = (Boolean) context.getProperty(RestUtil.PROPERTY_ENCODE_ENTITY);
        
        if (encode != null && encode && userMeta != null) {
            // backup original metadata in case of an error
            Map<String, String> metaBackup = new HashMap<String, String>(userMeta);
            
            try {
                // get pre-stream metadata from encoder
                DanglingOutputStream danglingStream = new DanglingOutputStream();
                OutputStream encodeStream = encodeChain.getEncodeStream(danglingStream, userMeta);
                
                // add pre-stream encode metadata to headers
                context.getHeaders().putAll(S3ObjectMetadata.getUmdHeaders(userMeta));
                
                // connect dangling stream to actual output and wrap with encoder
                OutputStream originalStream = context.getOutputStream();
                danglingStream.setOutputStream(originalStream);
                context.setOutputStream(encodeStream);
                
                context.proceed();
            } catch (RuntimeException e) {
                // restore metadata from backup
                userMeta.clear();
                userMeta.putAll(metaBackup);
                throw e;
            }
        } else {
            context.proceed();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        // get user metadata from response headers
        MultivaluedMap<String, String> headers = context.getHeaders();
        Map<String, String> storedMeta = S3ObjectMetadata.getUserMetadata(headers);
        Set<String> keysToRemove = new HashSet<String>();
        keysToRemove.addAll(storedMeta.keySet());
        
        // get encode specs from user metadata
        String[] encodeSpecs = CodecChain.getEncodeSpecs(storedMeta);
        if (encodeSpecs != null) {
            // create codec chain
            CodecChain decodeChain = new CodecChain(encodeSpecs).withProperties(codecProperties);
            
            // do we need to decode the entity?
            Boolean decode = (Boolean) context.getProperty(RestUtil.PROPERTY_DECODE_ENTITY);
            if (decode != null && decode) {
                // wrap input stream with decoder
                InputStream originalStream = context.getInputStream();
                InputStream decodeStream = decodeChain.getDecodeStream(originalStream, storedMeta);
                context.setInputStream(decodeStream);
            } else {
                // need to remove any encode metadata so we can update the headers
                decodeChain.removeEncodeMetadata(storedMeta, decodeChain.getEncodeMetadataList(storedMeta));
            }
            
            // should we keep the encode headers?
            Boolean keepHeaders = (Boolean) context.getProperty(RestUtil.PROPERTY_KEEP_ENCODE_HEADERS);
            if (keepHeaders == null || !keepHeaders) {
                // remove encode metadata from headers (storedMeta now contains only user-defined metadata)
                keysToRemove.removeAll(storedMeta.keySet()); // all metadata - user-defined metadata
                for (String key : keysToRemove) {
                    headers.remove(S3ObjectMetadata.getHeaderName(key));
                }
            }
        }
        
        return context.proceed();
    }


    public Map<String, Object> getCodecProperties() {
        return codecProperties;
    }

    public void setCodecProperties(Map<String, Object> codecProperties) {
        this.codecProperties = codecProperties;
    }

    public CodecFilter withCodecProperties(Map<String, Object> codecProperties) {
        setCodecProperties(codecProperties);
        return this;
    }

    private static class DanglingOutputStream extends FilterOutputStream {
        private static final OutputStream BOGUS_STREAM = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new RuntimeException("you didn't connect a dangling output stream!");
            }
        };

        DanglingOutputStream() {
            super(BOGUS_STREAM);
        }

        void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException("single-byte write called!");
        }
    }
}
