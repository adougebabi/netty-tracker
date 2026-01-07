package com.zhaoyang.boot.nettytest.protocol;

/**
 * 消息协议
 * 格式: TYPE|CLIENTID|PAYLOAD
 */
public class TrackerMessage {
    private MessageType type;
    private String clientId;
    private String payload;

    public enum MessageType {
        REGISTER,
        HEARTBEAT,
        MESSAGE,
        ACK,
        ERROR
    }

    public TrackerMessage() {
    }

    public TrackerMessage(MessageType type, String clientId, String payload) {
        this.type = type;
        this.clientId = clientId;
        this.payload = payload;
    }

    public static TrackerMessage parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String[] parts = raw.split("\\|", 3);
        if (parts.length < 2) {
            return null;
        }

        TrackerMessage message = new TrackerMessage();
        try {
            message.type = MessageType.valueOf(parts[0]);
            message.clientId = parts[1];
            message.payload = parts.length > 2 ? parts[2] : "";
        } catch (IllegalArgumentException e) {
            return null;
        }

        return message;
    }

    public String encode() {
        return String.format("%s|%s|%s\n", type, clientId, payload != null ? payload : "");
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return String.format("TrackerMessage{type=%s, clientId='%s', payload='%s'}",
                type, clientId, payload);
    }
}
