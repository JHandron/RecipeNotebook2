package com.recipenotebook.ui;

import com.recipenotebook.model.Recipe;
import com.recipenotebook.repository.RecipeRepository;
import org.bson.types.ObjectId;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainFrame extends JFrame {
    private final RecipeRepository repository;
    private final RecipeListPanel listPanel;
    private final RecipeEditorPanel editorPanel;
    private List<Recipe> allRecipes = new ArrayList<>();

    public MainFrame() {
        super("RecipeNotebook");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1100, 650));

        repository = new RecipeRepository();
        listPanel = new RecipeListPanel();
        editorPanel = new RecipeEditorPanel();
        editorPanel.setRelatedSelector(this::openRelatedDialog);
        editorPanel.setSaveListener(this::saveRecipe);
        editorPanel.setResetListener(this::resetEditorToSelection);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, editorPanel);
        splitPane.setResizeWeight(0.35);
        add(splitPane, BorderLayout.CENTER);

        attachListListeners();
        loadAllRecipes();
        pack();
        setLocationRelativeTo(null);
    }

    private void attachListListeners() {
        listPanel.addSearchListener(e -> performSearch());
        listPanel.addResetListener(e -> {
            listPanel.resetFilters();
            loadAllRecipes();
        });
        listPanel.addNewRecipeListener(e -> startNewRecipe());
        listPanel.addSelectionChangeListener(this::handleSelectionChange);
    }

    private void handleSelectionChange(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        Recipe selected = listPanel.getSelectedRecipe();
        if (selected != null) {
            editorPanel.displayRecipe(selected, allRecipes);
        }
    }

    private void startNewRecipe() {
        listPanel.clearSelection();
        Recipe newRecipe = new Recipe();
        editorPanel.displayRecipe(newRecipe, allRecipes);
    }

    private void performSearch() {
        try {
            allRecipes = repository.listAll();
            List<Recipe> results = repository.search(
                    listPanel.getNameQuery(),
                    listPanel.getTagFilters(),
                    listPanel.getIngredientKeywords());
            listPanel.updateList(results);
        } catch (Exception ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    private void loadAllRecipes() {
        try {
            allRecipes = repository.listAll();
            listPanel.updateList(allRecipes);
            if (!allRecipes.isEmpty()) {
                SwingUtilities.invokeLater(() -> listPanel.clearSelection());
            }
            startNewRecipe();
        } catch (Exception ex) {
            showError("Unable to load recipes: " + ex.getMessage());
        }
    }

    private void saveRecipe(Recipe recipe) {
        try {
            Recipe saved = repository.save(recipe);
            loadAllRecipes();
            selectRecipe(saved.getId());
            JOptionPane.showMessageDialog(this, "Recipe saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Unable to save recipe: " + ex.getMessage());
        }
    }

    private void selectRecipe(ObjectId id) {
        if (id == null) {
            return;
        }
        Optional<Recipe> match = allRecipes.stream().filter(r -> id.equals(r.getId())).findFirst();
        match.ifPresent(recipe -> SwingUtilities.invokeLater(() -> {
            listPanel.updateList(allRecipes);
            listPanel.selectRecipeById(id);
            editorPanel.displayRecipe(recipe, allRecipes);
        }));
    }

    private List<ObjectId> openRelatedDialog(ObjectId currentId, List<ObjectId> alreadySelected) {
        RelatedRecipeDialog dialog = new RelatedRecipeDialog(this, allRecipes, currentId, alreadySelected);
        dialog.setVisible(true);
        List<ObjectId> selected = dialog.getSelectedIds();
        if (selected == null || selected.isEmpty()) {
            return alreadySelected;
        }
        return selected;
    }

    private void resetEditorToSelection() {
        Recipe selected = listPanel.getSelectedRecipe();
        if (selected != null) {
            editorPanel.displayRecipe(selected, allRecipes);
        } else {
            startNewRecipe();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
