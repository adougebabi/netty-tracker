package com.zhaoyang.boot.nettytest;

import com.zhaoyang.boot.nettytest.protocol.TrackerMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for TrackerMessage protocol
 */
public class TrackerMessageTest {

    @Test
    public void testEncodeRegisterMessage() {
        TrackerMessage message = new TrackerMessage(
                TrackerMessage.MessageType.REGISTER,
                "CLIENT-001",
                "Test client"
        );

        String encoded = message.encode();
        assertEquals("REGISTER|CLIENT-001|Test client\n", encoded);
    }

    @Test
    public void testParseRegisterMessage() {
        String raw = "REGISTER|CLIENT-001|Test client";
        TrackerMessage message = TrackerMessage.parse(raw);

        assertNotNull(message);
        assertEquals(TrackerMessage.MessageType.REGISTER, message.getType());
        assertEquals("CLIENT-001", message.getClientId());
        assertEquals("Test client", message.getPayload());
    }

    @Test
    public void testEncodeHeartbeat() {
        TrackerMessage message = new TrackerMessage(
                TrackerMessage.MessageType.HEARTBEAT,
                "CLIENT-002",
                ""
        );

        String encoded = message.encode();
        assertEquals("HEARTBEAT|CLIENT-002|\n", encoded);
    }

    @Test
    public void testParseHeartbeat() {
        String raw = "HEARTBEAT|CLIENT-002|";
        TrackerMessage message = TrackerMessage.parse(raw);

        assertNotNull(message);
        assertEquals(TrackerMessage.MessageType.HEARTBEAT, message.getType());
        assertEquals("CLIENT-002", message.getClientId());
        assertEquals("", message.getPayload());
    }

    @Test
    public void testParseMessageWithPipe() {
        String raw = "MESSAGE|CLIENT-003|Hello|World";
        TrackerMessage message = TrackerMessage.parse(raw);

        assertNotNull(message);
        assertEquals(TrackerMessage.MessageType.MESSAGE, message.getType());
        assertEquals("CLIENT-003", message.getClientId());
        assertEquals("Hello|World", message.getPayload());
    }

    @Test
    public void testParseInvalidMessage() {
        String raw = "INVALID";
        TrackerMessage message = TrackerMessage.parse(raw);

        assertNull(message);
    }

    @Test
    public void testParseInvalidType() {
        String raw = "UNKNOWN|CLIENT-004|Test";
        TrackerMessage message = TrackerMessage.parse(raw);

        assertNull(message);
    }

    @Test
    public void testParseEmptyMessage() {
        String raw = "";
        TrackerMessage message = TrackerMessage.parse(raw);

        assertNull(message);
    }

    @Test
    public void testParseNullMessage() {
        TrackerMessage message = TrackerMessage.parse(null);

        assertNull(message);
    }

    @Test
    public void testRoundTrip() {
        TrackerMessage original = new TrackerMessage(
                TrackerMessage.MessageType.MESSAGE,
                "CLIENT-005",
                "Round trip test"
        );

        String encoded = original.encode().trim();
        TrackerMessage parsed = TrackerMessage.parse(encoded);

        assertNotNull(parsed);
        assertEquals(original.getType(), parsed.getType());
        assertEquals(original.getClientId(), parsed.getClientId());
        assertEquals(original.getPayload(), parsed.getPayload());
    }
}
