package com.chat.utils.message;

public record Message (MessageType type, String payload, String sender, String receiver) {


    public Message() {
        this(null, null, null, null);
    }
    public Message(MessageType type, String payload) {
        this(type, payload, null, null);
    }
    public Message(MessageType type, String payload, String sender) {
        this(type, payload, sender, null);
    }
}