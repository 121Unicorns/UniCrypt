package com.unicorn.unicrypt;

import java.util.Comparator;
import java.util.Map;

public class ChatMessage {
    private String sender;
    private String receiver;
    private String message;
    private long messageTime;
    private int type;

    public ChatMessage() {
    }

    public ChatMessage(String sender, String receiver, String message, long messageTime) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.messageTime = messageTime;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long timestamp) {
        this.messageTime = timestamp;
    }
}