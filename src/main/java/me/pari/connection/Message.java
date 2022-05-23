package me.pari.connection;

import com.google.gson.annotations.Expose;

public class Message {

    @Expose
    private final int msgId;

    @Expose
    private final String text;

    @Expose
    private final String username;

    public Message(int msgId, String text, String username) {
        this.msgId = msgId;
        this.text = text;
        this.username = username;
    }

    public int getMsgId() {
        return msgId;
    }

    public String getText() {
        return text;
    }

    public String getUsername() {
        return username;
    }
}
