package me.pari;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.hydev.logger.HyLogger;
import org.json.JSONObject;

import java.io.*;

public class Settings {

    // Logger
    private static final HyLogger LOGGER = new HyLogger("Settings");

    // Connection info
    @Expose
    private String hostName;

    @Expose
    private Integer port;

    // Auth user info
    @Expose
    private String username;

    @Expose
    private String authToken;

    // All info
    private final File file;

    public Settings(String fileName) {
        file = new File(fileName);
    }

    public void dumpJson() {
        try (BufferedWriter settingsFile = new BufferedWriter(new FileWriter(file))) {
            settingsFile.write(
                    new JSONObject()
                            .put("hostName", hostName)
                            .put("port", port)
                            .put("username", username)
                            .put("authToken", authToken)
                            .toString()
            );
        } catch (IOException e) {
            LOGGER.error("Error dumping json: " + e.getMessage());
        }
    }

    public void loadJson() {
        try (BufferedReader settingsFile = new BufferedReader(new FileReader(file))) {

            // Load string from file
            StringBuilder s = new StringBuilder();
            int c;
            while ((c = settingsFile.read()) >= 0)
                s.append((char) c);

            // Load new settings
            Settings sett = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(s.toString(), this.getClass());
            hostName = sett.hostName;
            port = sett.port;
            username = sett.username;
            authToken = sett.authToken;

        } catch (IOException e) {
            LOGGER.error("Error loading json: " + e.getMessage());
        }
    }

    public boolean exists() {
        return file.exists();
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
