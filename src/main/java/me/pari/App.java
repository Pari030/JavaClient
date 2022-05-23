package me.pari;

import me.pari.connection.Client;
import me.pari.connection.Message;
import me.pari.connection.Response;
import me.pari.connection.Status;
import me.pari.dialogs.AuthDialog;
import org.hydev.logger.HyLogger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class App extends javax.swing.JFrame {

    // Logger
    private static final HyLogger LOGGER = new HyLogger("App");

    private javax.swing.JLabel loggedAsLabel;
    private javax.swing.JLabel activeUsersLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    // private javax.swing.JButton logOutButton;
    private javax.swing.JList<String> messageList;
    private DefaultListModel<String> messageListModel = new DefaultListModel<>();
    private javax.swing.JButton sendMessageButton;
    private javax.swing.JTextField sendMessageField;
    private Client client;
    private final Settings settings;

    public App(@NotNull Settings settings) {
        initComponents();

        // Get server info by settings
        this.settings = settings;
        String hostname = settings.getHostName();
        Integer port = settings.getPort();

        // Check the server info
        do {

            // Hostname or port not defined
            if (hostname == null || port == null)

                // Ask for host and port and set into settings
                if (!getHostFlowDialog())

                    // The user exited
                    System.exit(0);

            // Check (new) hostname and password
            hostname = settings.getHostName();
            port = settings.getPort();

            // Create and connect the client
            try {
                client = new Client(hostname, port);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "The server you are retrying to connect is not available: " + ex.getMessage(),
                        "Connection Error",
                        JOptionPane.WARNING_MESSAGE
                );
                hostname = null;
                port = null;
            }

        } while (client == null);

        // Get all data from socket and put in responses
        new Thread(() -> client.responseUpdater()).start();

        // Get user info
        String username = settings.getUsername();
        String authToken = settings.getAuthToken();

        // Not authorized via auth token
        if (!(authToken != null && authFlow(authToken)))

            // Login or SignUp via username and password
            if (!authFlowDialog(username))
                System.exit(0);

        updateLabels();

        // Now the client is authenticated and is all ok
        client.startUpdater(settings);

        // New messages updater
        new Thread(this::messageUpdater).start();
    }

    private void messageUpdater() {
        while (!client.isClosed()) {

            // Check new messaged
            if (!client.hasNewMessage()) {
                Thread.onSpinWait();
                continue;
            }

            // New message
            Message m = client.getNewMessage();
            messageListModel.addElement(m.getUsername() + ": " + m.getText());
            updateLabels();
        }

        // The client closed due to some error
        if (client.isUnexpectedClosed()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The socket closed due to some unexpected error.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }

    }

    private boolean getHostFlowDialog() {
        String hostname = null;
        Integer port = null;

        do {
            String address = (String) JOptionPane.showInputDialog(
                    this,
                    "Insert the address of the server to connect: ",
                    "ChatServer address",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    "https://127.0.0.1:7777"
            );

            // The user closed the tab
            if (address == null)
                return false;

            // Split server info
            address = address.replace("https://", "").replace("http://", "");
            String[] serverInfo = address.split(":", 2);

            // Invalid server info
            if (serverInfo.length != 2) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid address",
                        "The address you wrote is invalid.",
                        JOptionPane.WARNING_MESSAGE
                );
                continue;
            }

            // Extract hostname and port
            hostname = serverInfo[0];
            try {
                port = Integer.parseInt(serverInfo[1]);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid port",
                        "Port is invalid",
                        JOptionPane.WARNING_MESSAGE
                );
            }

        } while (hostname == null || port == null);

        // Update settings
        settings.setHostName(hostname);
        settings.setPort(port);
        settings.dumpJson();
        return true;
    }

    private boolean authFlowDialog(String username) {

        // Sent auth dialog
        AuthDialog dialog = new AuthDialog(this, username);

        // User not quit
        if (!dialog.hasQuit()) {

            // User logged in / signed up
            if (authFlow(dialog.getUsername(), dialog.getPassword(), dialog.hasSignedUp()))
                return true;

            // Error during login / signup
            return authFlowDialog(username);
        }

        // Not logged in, the user exited or internal error
        return false;
    }

    private boolean authFlow(@NotNull String username, @NotNull String password, boolean isSignUp) {

        // Check if the username and password are valid
        Response r;
        try {
            if (isSignUp)
                r = client.createUser(username, password);
            else
                r = client.authUser(username, password);

        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "The server is having internal issues, try later.",
                    "Server is lagging",
                    JOptionPane.WARNING_MESSAGE
            );
            System.exit(0);
            return false;

        } catch (IOException e) {
            LOGGER.error("IOException during authFlow via username and password: " + e.getMessage());
            return false;
        }

        // Check response status
        switch (r.getStatus()) {

            case Status.OK -> {
                settings.setAuthToken(r.getValues().get("authToken"));
                settings.setUsername(username);
                settings.dumpJson();
                return true;
            }

            case Status.BAD_REQUEST -> {
                settings.setAuthToken(null);
                settings.dumpJson();
                JOptionPane.showMessageDialog(
                        this,
                        "Bad Request error: " + r.getDesc(),
                        "Authentication Error",
                        JOptionPane.WARNING_MESSAGE
                );
            }

            default -> {
                LOGGER.warning("Unhandled response in authFlow: " + r.getStatus() + " - " + r.getDesc());
                JOptionPane.showMessageDialog(
                        this,
                        "Server responded with an unexpected error: " + r.getDesc(),
                        "Server Error",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }

        return false;
    }

    private boolean authFlow(@NotNull String authToken) {

        // Check if the authToken is valid
        Response r;
        try {
            r = client.authUser(authToken);

        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "The server is having internal issues, try later.",
                    "Server is lagging",
                    JOptionPane.WARNING_MESSAGE
            );
            System.exit(0);
            return false;

        } catch (IOException e) {
            LOGGER.error("IOException during authFlow via authToken: " + e.getMessage());
            return false;
        }

        // Check response status
        switch (r.getStatus()) {

            case Status.OK -> {
                settings.setAuthToken(r.getValues().get("authToken"));
                settings.dumpJson();
                return true;
            }

            case Status.BAD_REQUEST -> {
                settings.setAuthToken(null);
                settings.dumpJson();
                JOptionPane.showMessageDialog(
                        this,
                        "The authToken expired, you need to login again.",
                        "Authentication Error",
                        JOptionPane.WARNING_MESSAGE
                );
                return false;
            }

            default -> {
                LOGGER.warning("Unhandled response in authFlow: " + r.getStatus() + " - " + r.getDesc());
                JOptionPane.showMessageDialog(
                        this,
                        "Server responded with an unexpected error: " + r.getDesc(),
                        "Server Error",
                        JOptionPane.WARNING_MESSAGE
                );
                return false;
            }
        }
    }

    /**
     * Private UX/UI update methods
     * */

    private void updateLabels() {
        loggedAsLabel.setText("Logged as " + settings.getUsername());
        try {
            activeUsersLabel.setText("Active users: " + client.getUsers().getValues().get("count"));
        } catch (TimeoutException | IOException e) {
            activeUsersLabel.setText("Active users: IDK");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {

        SwingUtilities.updateComponentTreeUI(this);
        setTitle("JavaClient 1.0");

        // Initialize components
        sendMessageButton = new javax.swing.JButton();
        sendMessageField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        messageList = new javax.swing.JList<>();
        loggedAsLabel = new javax.swing.JLabel();
        activeUsersLabel = new javax.swing.JLabel();
        // logOutButton = new javax.swing.JButton();

        // Set close operation
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                client.close(false);
            }

        });

        // Send message component
        sendMessageButton.setText("Send");
        sendMessageButton.addActionListener(this::sendMessageButtonActionPerformed);
        getRootPane().setDefaultButton(sendMessageButton);

        /* Logout component
        logOutButton.setText("Logout");
        logOutButton.addActionListener(this::logOutButtonActionPerformed);
        */

        messageList.setModel(messageListModel);

        jScrollPane1.setViewportView(messageList);

        loggedAsLabel.setText("Logged as: {}");

        activeUsersLabel.setText("Active users: {}");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sendMessageField)
                        .addGap(18, 18, 18)
                        .addComponent(sendMessageButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(loggedAsLabel)
                        .addGap(60, 60, 60)
                        .addComponent(activeUsersLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 111, Short.MAX_VALUE)
                        // .addComponent(logOutButton)
                    ))
                .addContainerGap())
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loggedAsLabel)
                    .addComponent(activeUsersLabel)
                    // .addComponent(logOutButton)
                )
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendMessageButton)
                    .addComponent(sendMessageField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }

    /**
    * UI Button methods
    * */

    private void sendMessageButtonActionPerformed(java.awt.event.ActionEvent ignored) {
        // Text
        String text = sendMessageField.getText();

        // Text empty
        if (text == null || text.isBlank())
            return;

        // Text too long
        if (text.length() >= 1024) {
            JOptionPane.showMessageDialog(
                    this,
                    "Your message text is too long.",
                    "Text too long", JOptionPane.WARNING_MESSAGE
            );
            return;
        }


        Response r;
        try {
            r = client.sendMessage(text);

        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "The server is having internal issues, try later.",
                    "Server is lagging",
                    JOptionPane.WARNING_MESSAGE
            );
            System.exit(0);
            return;

        } catch (IOException e) {
            LOGGER.error("IOException during sendMessage: " + e.getMessage());
            return;
        }

        if (r.getStatus() != 200) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error send message: " + r.getDesc(),
                    "Message Error",
                    JOptionPane.WARNING_MESSAGE
            );
        }

        sendMessageField.setText("");
    }
}
