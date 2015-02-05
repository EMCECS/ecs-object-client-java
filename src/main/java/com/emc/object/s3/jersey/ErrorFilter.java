/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3Exception;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.apache.log4j.LogMF;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import java.io.InputStreamReader;
import java.io.Reader;

public class ErrorFilter extends ClientFilter {
    private static final Logger l4j = Logger.getLogger(ErrorFilter.class);

    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        ClientResponse response = getNext().handle(request);

        if (response.getStatus() > 299) {
            throw parseErrorResponse(new InputStreamReader(response.getEntityInputStream()), response.getStatus());
        }

        return response;
    }

    public static S3Exception parseErrorResponse(Reader reader, int statusCode) {

        // JAXB will expect a namespace if we try to unmarshall, but some error responses don't include
        // a namespace. In lieu of writing a SAXFilter to apply a default namespace in-line, this works just as well.
        SAXBuilder sb = new SAXBuilder();

        Document d;
        try (Reader r = reader) {
            d = sb.build(r);
        } catch (Throwable t) {
            return new S3Exception("could not parse error response", statusCode, t);
        }

        String code = d.getRootElement().getChildText("Code");
        if (code == null)
            code = d.getRootElement().getChildText("Code", Namespace.getNamespace(S3Constants.XML_NAMESPACE));

        String message = d.getRootElement().getChildText("Message");
        if (message == null)
            message = d.getRootElement().getChildText("Message", Namespace.getNamespace(S3Constants.XML_NAMESPACE));

        String requestId = d.getRootElement().getChildText("RequestId");
        if (requestId == null)
            requestId = d.getRootElement().getChildText("RequestId", Namespace.getNamespace(S3Constants.XML_NAMESPACE));

        if (code == null && message == null) {
            // not an error from S3
            return new S3Exception("no code or message in error response", statusCode);
        }

        LogMF.debug(l4j, "Error: {0}, message: {1}, requestId: {2}", code, message, requestId);
        return new S3Exception(message, statusCode, code, requestId);
    }
}
