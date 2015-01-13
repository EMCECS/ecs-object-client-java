package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3Exception;
import org.apache.log4j.LogMF;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.io.IOException;

public class ErrorResponseFilter implements ClientResponseFilter {
    private static final Logger l4j = Logger.getLogger(ErrorResponseFilter.class);

    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        if (responseContext.getStatus() > 299) {

            // JAXB will expect a namespace if we try to unmarshall, but some error responses don't include
            // a namespace. In lieu of writing a SAXFilter to apply a default namespace in-line, this works just as well.
            SAXBuilder sb = new SAXBuilder();

            Document d;
            try {
                d = sb.build(responseContext.getEntityStream());
            } catch (Throwable t) {
                throw new S3Exception(responseContext.getStatusInfo().getReasonPhrase(), responseContext.getStatus());
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
                throw new S3Exception(responseContext.getStatusInfo().getReasonPhrase(), responseContext.getStatus());
            }

            LogMF.debug(l4j, "Error: {0}, message: {1}, requestId: {2}", code, message, requestId);
            throw new S3Exception(message, responseContext.getStatus(), code, requestId);
        }
    }
}
