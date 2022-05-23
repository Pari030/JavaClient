package me.pari.connection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import me.pari.Settings;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.net.SocketException;
import java.util.*;
import java.util.Timer;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Client extends AbstractClient {

    /*
    * Reader-Thread:
    *   Reads json and put it into responses.
    *
    *
    * */

    private final LinkedList<Response> responses = new LinkedList<>();
    private final LinkedList<Message> messages = new LinkedList<>();
    private final Gson j = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static final int RETRIES = 15;

    public Client(String hostname, int port) throws IOException {
        super(hostname, port);
    }

    private class UpdateAuthTokenTask extends TimerTask {

        private final Settings sett;

        public UpdateAuthTokenTask(Settings sett) {
            super();
            this.sett = sett;
        }

        @Override
        public void run() {
            Response r;
            try {
                r = authUser(sett.getAuthToken());
            } catch (IOException | TimeoutException e) {
                return;
            }

            switch (r.getStatus()) {

                case Status.OK -> {
                    sett.setAuthToken(r.getValues().get("authToken"));
                    sett.dumpJson();
                }

                case Status.BAD_REQUEST -> {
                    sett.setAuthToken(null);
                    sett.dumpJson();
                }

                default -> LOGGER.warning("Unhandled response in UpdateAuthTokenTask: " + r.getStatus() + " - " + r.getDesc());
            }

            r.getValues().get("authToken");
        }
    }

    public void startUpdater(Settings sett) {
        Timer time = new Timer();
        UpdateAuthTokenTask st = new UpdateAuthTokenTask(sett);
        time.schedule(st, 20*60*1000, 20*60*1000);
    }

    public void responseUpdater() {
        JSONObject j;
        Gson g = new Gson();
        while (isConnected.get()) {
            try {
                j = readJson();
                LOGGER.log("New json" + j);

                if (j.has("id"))
                    responses.add(g.fromJson(j.toString(), Response.class));
                else
                    messages.add(g.fromJson(j.toString(), Message.class));

            } catch (JsonSyntaxException ex) {
                LOGGER.log("Bad Packet sent by server.");

            } catch (SocketException ex) {
                LOGGER.log("Client disconnected due to: " + ex.getMessage());

                // Unexpected closed by thread
                close(true);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private synchronized Response getResponse(int id) throws TimeoutException {
        for (int i = 0; i < RETRIES; i++) {
            for (Response r : responses)
                if (r.getId() == id) {
                    responses.remove(r);
                    return r;
                }
            try {Thread.sleep(300);} catch (InterruptedException ignored) {}
        }
        throw new TimeoutException("Response not found");
    }

    private Response sendRequest(@NotNull String method, HashMap<String, String> params) throws TimeoutException, IOException {
        int id = generateId();
        Request r = new Request(id, method, params);
        sendJson(new JSONObject(j.toJson(r)));
        return getResponse(id);
    }

    private int generateId() {
        try {
            return responses.getLast().getId()+1;
        } catch (NoSuchElementException e){
            return 1;
        }
    }

    public Message getNewMessage() {
        try {
            return messages.pop();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public boolean hasNewMessage() {
        return messages.size() > 0;
    }

    /**
     * Server Protocol API
     */

    public Response authUser(String authToken) throws TimeoutException, IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put("authToken", authToken);
        return sendRequest("authUser", params);
    }

    public Response authUser(String username, String password) throws TimeoutException, IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        return sendRequest("authUser", params);
    }

    public Response createUser(String username, String password) throws TimeoutException, IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        return sendRequest("createUser", params);
    }

    public Response sendMessage(String text) throws TimeoutException, IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put("text", text);
        return sendRequest("sendMessage", params);
    }

    public Response ping() throws TimeoutException, IOException {
        return sendRequest("ping", null);
    }

    public Response getMessages(int limit, int offset) throws TimeoutException {
        return getResponse(1);
    }

    public Response getUsers() throws TimeoutException, IOException {
        return sendRequest("getUsers", null);
    }

}
