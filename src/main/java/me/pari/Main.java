package me.pari;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class Main {

    /*
    *
    * */

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException exc) {
            System.err.println("Nimbus: Unsupported Look and feel!");
        }

        // Get settings
        final Settings settings = new Settings("settings.json");

        // Check if is there settings, if not create new
        if (!settings.exists())
            settings.dumpJson();
        settings.loadJson();

        // Create and display the form
        java.awt.EventQueue.invokeLater(() -> new App(settings).setVisible(true));
    }
}
