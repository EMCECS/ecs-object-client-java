/*
 * Copyright (c) 2015, EMC Corporation.
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

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pins the Glassfish JAXB RI for {@code com.emc.object.s3.bean} classes, so that S3 XML
 * marshalling is not affected by other JAXB providers (e.g. MOXy) on the classpath.
 *
 * @see S3JerseyClient
 */
@Provider
public class S3JaxbContextResolver implements ContextResolver<JAXBContext> {

    private static final Logger log = LoggerFactory.getLogger(S3JaxbContextResolver.class);
    private static final String S3_BEAN_PACKAGE = "com.emc.object.s3.bean";
    private static final String RI_FACTORY_CLASS = "org.glassfish.jaxb.runtime.v2.JAXBContextFactory";

    private final jakarta.xml.bind.JAXBContextFactory riFactory;
    private final Map<Class<?>, JAXBContext> contextCache = new ConcurrentHashMap<>();

    public S3JaxbContextResolver() {
        try {
            Class<?> factoryClass = Class.forName(RI_FACTORY_CLASS);
            riFactory = (jakarta.xml.bind.JAXBContextFactory) factoryClass.getDeclaredConstructor().newInstance();
            log.debug("S3JaxbContextResolver: using Glassfish RI for S3 bean JAXB contexts");
        } catch (Exception e) {
            throw new RuntimeException("Glassfish JAXB RI (" + RI_FACTORY_CLASS + ") is required on the classpath "
                    + "for correct S3 XML marshalling. Add org.glassfish.jaxb:jaxb-runtime as a dependency.", e);
        }
    }

    @Override
    public JAXBContext getContext(Class<?> type) {
        if (type == null || !type.getPackageName().equals(S3_BEAN_PACKAGE)) {
            return null;
        }
        return contextCache.computeIfAbsent(type, this::createRiContext);
    }

    private JAXBContext createRiContext(Class<?> type) {
        try {
            return riFactory.createContext(new Class<?>[]{type}, null);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to create Glassfish RI JAXBContext for " + type.getName(), e);
        }
    }
}
