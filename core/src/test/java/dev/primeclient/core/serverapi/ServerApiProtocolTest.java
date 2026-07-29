package dev.stellarclient.core.serverapi;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerApiProtocolTest {

    @Test
    void parsesType() {
        JsonObject obj = ServerApiProtocol.parse("{\"t\":\"HANDSHAKE\",\"protocol\":1}");
        assertEquals("HANDSHAKE", ServerApiProtocol.typeOf(obj));
        assertEquals(1, ServerApiProtocol.integer(obj, "protocol", 0));
    }
}
