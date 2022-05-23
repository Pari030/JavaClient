package me.pari.connection;

import com.google.gson.annotations.Expose;

import java.util.HashMap;

public class Request {
    @Expose
    private final int id;

    @Expose
    private final String method;

    @Expose
    private final HashMap<String, String> params;

     public Request(int id, String method, HashMap<String, String> params) {
         this.id = id;
         this.method = method;
         this.params = params;
     }

}
