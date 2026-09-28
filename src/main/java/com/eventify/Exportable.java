package com.eventify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface demonstrating an advanced OOP concept.
 * Any class implementing Exportable must be able to serialize itself to a JSON ObjectNode.
 */
public interface Exportable {
    ObjectNode toJsonNode(ObjectMapper mapper);
}
