package org.jhandron;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import org.jhandron.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

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
            //Note: Invoke this method before setting the look and feel.
            FlatLaf.registerCustomDefaultsSource("theme");
//            UIManager.put("TabbedPane.showTabSeparators", true);
//            UIManager.put("SplitPane.oneTouchButtonOffset", 5 );
            FlatIntelliJLaf.setup();
            FlatLaf.setUseNativeWindowDecorations(true);
//            UIManager.put("TabbedPane.selectedBackground", Color.GRAY);
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex.getMessage());
        }
    }
}
