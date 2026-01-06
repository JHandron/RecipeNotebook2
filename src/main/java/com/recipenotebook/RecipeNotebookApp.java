package com.recipenotebook;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.recipenotebook.ui.MainFrame;

import javax.swing.SwingUtilities;

public final class RecipeNotebookApp {
    private RecipeNotebookApp() {
    }

    public static void main(String[] args) {
        setupLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    private static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
            FlatLaf.setUseNativeWindowDecorations(true);
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex.getMessage());
        }
    }
}
