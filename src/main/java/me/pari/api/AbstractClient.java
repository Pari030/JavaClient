package me.pari.api;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import me.pari.Utils;
import me.pari.api.errors.ConnectionError;
import me.pari.connection.Message;
import me.pari.connection.Response;
import org.hydev.logger.HyLogger;
import org.json.JSONObject;

public abstract class AbstractClient {

    // Logger & Utils
    protected static final HyLogger LOGGER = new HyLogger("Client");
    private static final Gson g = new Gson();

    // Data stream socket
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;
    private JSONObject j;

    // Buffered cache
    protected final LinkedList<Response> responses = new LinkedList<>();
    protected final LinkedList<Message> messages = new LinkedList<>();

    // Connection info
    private final String hostName;
    private final Integer port;

    protected final AtomicBoolean isConnected = new AtomicBoolean(true);
    protected final AtomicBoolean isClosedUnexpected = new AtomicBoolean(false);

    protected AbstractClient(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    /**
     * Connect and start inputUpdater thread
     * */
    protected void connect() throws IOException {

        // Client already connected
        if (isConnected.get())
            throw new ConnectionError();

        // Connect to the server
        socket = new Socket(hostName, port);
        output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        // Turn on connection
        isConnected.set(true);

        // Start the input updater thread
        new Thread(() -> {
            while (isConnected.get()) {
                try {
                    j = readJson();
                    LOGGER.log("New json: " + j);

                    if (j.has("id"))
                        responses.add(g.fromJson(j.toString(), Response.class));
                    else
                        messages.add(g.fromJson(j.toString(), Message.class));

                } catch (JsonSyntaxException ex) {
                    LOGGER.log("Bad packet sent by server.");

                } catch (SocketException ex) {
                    LOGGER.log("Client disconnected due to: " + ex.getMessage());

                    // Unexpected closed by thread
                    if (isConnected.get())
                        disconnect(true);

                } catch (IOException ex) {
                    LOGGER.error("IOException during read of input: " + ex.getMessage());
                }
            }
        });
    }

    // Low level read json
    protected JSONObject readJson() throws IOException {

        // Create a buffer
        int c;
        StringBuilder buff = new StringBuilder();

        // Read stream until json is created
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

    // Low level send json
    protected void sendJson(JSONObject json) throws IOException {
        output.writeBytes(json.toString());
        output.flush();
    }

    protected void disconnect(boolean unexpected) {
        try {
            socket.close();
        } catch (IOException ex) {
            LOGGER.error("Error closing client: " + ex.getMessage());
        } finally {
            isConnected.set(false);
            isClosedUnexpected.set(unexpected);
        }
    }
}
