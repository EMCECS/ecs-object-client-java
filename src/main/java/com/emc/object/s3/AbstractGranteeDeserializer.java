package com.emc.object.s3;

import com.emc.object.s3.bean.AbstractGrantee;
import com.emc.object.s3.bean.CanonicalUser;
import com.emc.object.s3.bean.Group;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class AbstractGranteeDeserializer extends JsonDeserializer<AbstractGrantee> {
    @Override
    public AbstractGrantee deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode idNode = node.get("ID");
        JsonNode displayNameNode = node.get("DisplayName");
        if (idNode != null && displayNameNode != null) {
            return new CanonicalUser(idNode.asText(), displayNameNode.asText());
        } else if (node.get("uri") != null) {
            return new Group(node.get("uri").asText());
        } else {
            throw new JsonParseException(jp, "Could not determine subtype for AbstractGrantee");
        }
    }
}
