package me.pari.connection;

import com.google.gson.annotations.Expose;

import java.util.HashMap;

public class Response {

    @Expose
    private final int id;
    private final int status;
    private final String desc;
    private final HashMap<String, String> values;

    public Response(int id, int status, String desc, HashMap<String, String> values) {
        this.id = id;
        this.status = status;
        this.desc = desc;
        this.values = values;
    }

    public int getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public HashMap<String, String> getValues() {
        return values;
    }
}
