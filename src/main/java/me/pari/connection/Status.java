package me.pari.connection;

public class Status {

    // Ok
    public static final int OK = 200;

    // Bad requests
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int FLOOD = 420;

    // Internal errors
    public static final int INTERNAL_ERROR = 500;

}
