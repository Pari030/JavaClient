package me.pari.connection;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import me.pari.Utils;
import org.hydev.logger.HyLogger;
import org.json.JSONObject;

public abstract class AbstractClient {

    // Logger
    protected static final HyLogger LOGGER = new HyLogger("Client");
    
    // Data stream socket vars
    private final Socket socket;
    private final DataOutputStream output;
    private final DataInputStream input;

    protected final AtomicBoolean isConnected = new AtomicBoolean(true);
    protected final AtomicBoolean isClosedUnexpected = new AtomicBoolean(false);

    public AbstractClient(String hostname, int port) throws IOException {
        socket = new Socket(hostname, port);
        output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

    }

    protected JSONObject readJson() throws IOException {
        // Create a buffer
        int c;
        StringBuilder buff = new StringBuilder();

        do {
            c = input.read();

            // The client disconnected
            if (c == -1)
                throw new SocketException("Connection closed");

            // Append to the buffer
            buff.append((char) c);
        } while (!Utils.isJson(buff.toString()));

        return new JSONObject(buff.toString());
    }

    protected void sendJson(JSONObject json) throws IOException {
        output.writeBytes(json.toString());
        output.flush();
    }

    public boolean isClosed() {
        return !isConnected.get();
    }

    public boolean isUnexpectedClosed() {
        return isClosedUnexpected.get() && !isConnected.get();
    }

    public void close(boolean unexpected) {
        try {
            socket.close();
        } catch (IOException ignored) {

        } finally {
            isConnected.set(false);
            isClosedUnexpected.set(unexpected);
        }
    }
}
