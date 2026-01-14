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

    public MainFrame(boolean p_testEnvironment) {
        super("Recipe Notebook");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1100, 650));

        repository = new RecipeRepository(p_testEnvironment);
        listPanel = new RecipeListPanel();
        editorPanel = new RecipeEditorPanel();
        editorPanel.setRelatedSelector(this::openRelatedDialog);
        editorPanel.setSaveListener(this::saveRecipe);
        editorPanel.setResetListener(this::resetEditorToSelection);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, editorPanel);
        listPanel.setMinimumSize(new Dimension(240, 200));
        editorPanel.setMinimumSize(new Dimension(520, 200));
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerSize(10);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        attachListListeners();
        loadAllRecipes();
        pack();
        setLocationRelativeTo(null);
    }

    private void attachListListeners() {
        listPanel.addFilterChangeListener(this::applyFilter);
        listPanel.addResetListener(e -> {
            listPanel.resetFilters();
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

    private void loadAllRecipes() {
        try {
            allRecipes = repository.listAll();
            applyFilter();
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
            List<Recipe> filtered = applyFilter();
            if (filtered.stream().anyMatch(item -> id.equals(item.getId()))) {
                listPanel.selectRecipeById(id);
            }
            editorPanel.displayRecipe(recipe, allRecipes);
        }));
    }

    private List<Recipe> applyFilter() {
        String query = listPanel.getFilterText();
        if (query.isBlank()) {
            listPanel.updateList(allRecipes);
            return allRecipes;
        }
        RecipeListPanel.FilterType filterType = listPanel.getSelectedFilterType();
        List<String> tokens = parseTokens(query);
        List<Recipe> filtered = allRecipes.stream()
                .filter(recipe -> matchesFilter(recipe, filterType, query, tokens))
                .toList();
        listPanel.updateList(filtered);
        return filtered;
    }

    private boolean matchesFilter(Recipe recipe, RecipeListPanel.FilterType filterType, String query,
                                  List<String> tokens) {
        if (recipe == null) {
            return false;
        }
        switch (filterType) {
            case INGREDIENTS -> {
                return matchesIngredientTokens(recipe, tokens);
            }
            case TAGS -> {
                return matchesTagTokens(recipe, tokens);
            }
            case NAME -> {
                String name = recipe.getName();
                return name != null && name.toLowerCase().contains(query.toLowerCase());
            }
            default -> {
                return true;
            }
        }
    }

    private boolean matchesTagTokens(Recipe recipe, List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        List<String> tags = recipe.getTags();
        return tokens.stream().allMatch(token -> tags.stream()
                .anyMatch(tag -> tag != null && tag.toLowerCase().contains(token)));
    }

    private boolean matchesIngredientTokens(Recipe recipe, List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        List<String> ingredients = recipe.getIngredients();
        return tokens.stream().anyMatch(token -> ingredients.stream()
                .anyMatch(ingredient -> ingredient != null && ingredient.toLowerCase().contains(token)));
    }

    private List<String> parseTokens(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(query.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(String::toLowerCase)
                .toList();
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
