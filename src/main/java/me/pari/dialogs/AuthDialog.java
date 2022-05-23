package me.pari.dialogs;

import javax.swing.*;
import java.awt.*;

public class AuthDialog extends JDialog {

    private final JLabel statusLabel = new JLabel(" ");

    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField();

    private boolean quit = true;
    private boolean signedUp = false;

    public AuthDialog(final JFrame parent, String username) {
        super(parent, true);

        GridLayout grid = new GridLayout(5, 1);
        setTitle("Authentication");
        usernameField.setText(username);

        JPanel inputPanel = new JPanel(grid);

        // Add username label + field
        JLabel usernameLabel = new JLabel("Username");
        inputPanel.add(usernameLabel);
        inputPanel.add(usernameField);

        // Add blank separator
        JSeparator js = new JSeparator();
        js.setVisible(false);
        inputPanel.add(js);

        // Add password label + field
        JLabel passwordLabel = new JLabel("Password");
        inputPanel.add(passwordLabel);
        inputPanel.add(passwordField);

        JPanel totalInputPanel = new JPanel();
        totalInputPanel.add(inputPanel);

        JPanel buttonsPanel = new JPanel();
        JButton loginButton = new JButton("Login");
        buttonsPanel.add(loginButton);
        JButton signUpButton = new JButton("Sign Up");
        buttonsPanel.add(signUpButton);

        JPanel totalButtonsPanel = new JPanel(new BorderLayout());
        totalButtonsPanel.add(buttonsPanel, BorderLayout.CENTER);
        totalButtonsPanel.add(statusLabel, BorderLayout.NORTH);

        statusLabel.setForeground(Color.RED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        setLayout(new BorderLayout());

        // Add panels to the authDialog
        add(totalInputPanel, BorderLayout.CENTER);
        add(totalButtonsPanel, BorderLayout.SOUTH);
        pack();

        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Set size
        Dimension d = new Dimension(340, 220);
        setSize(d);
        setMinimumSize(d);

        loginButton.addActionListener(e -> {
            if (inputNotFilled())
                return;
            quit = false;
            signedUp = false;
            setVisible(false);
            dispose();
        });

        signUpButton.addActionListener(e -> {
            if (inputNotFilled())
                return;
            quit = false;
            signedUp = true;
            setVisible(false);
            dispose();
        });

        setVisible(true);
    }

    private boolean inputNotFilled() {
        // Username is empty
        if (usernameField.getText().isEmpty()) {
            statusLabel.setText("Username is blank.");
            return true;
        }

        // Password is empty
        if (passwordField.getPassword().length == 0) {
            statusLabel.setText("Password is blank.");
            return true;
        }

        return false;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return String.valueOf(passwordField.getPassword());
    }

    public boolean hasQuit() {
        return quit;
    }

    public boolean hasSignedUp() {
        return signedUp;
    }

}
