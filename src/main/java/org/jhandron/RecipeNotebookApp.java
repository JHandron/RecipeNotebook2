package org.jhandron;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.jhandron.ui.MainFrame;

import javax.swing.SwingUtilities;

public final class RecipeNotebookApp {


    private RecipeNotebookApp() {
    }

    public static void main(String[] args) {
        setupLookAndFeel();

        final boolean testEnvironment;
        if (args.length > 0) {
            testEnvironment = "DEV".equals(args[0]);
        } else {
            testEnvironment = false;
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(testEnvironment);
            frame.setVisible(true);
        });

    }

    private static void setupLookAndFeel() {
        try {
            FlatIntelliJLaf.setup();
            FlatLaf.setUseNativeWindowDecorations(true);
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex.getMessage());
        }
    }
}
