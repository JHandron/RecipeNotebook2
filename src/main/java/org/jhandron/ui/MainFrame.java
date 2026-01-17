package org.jhandron.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.jhandron.model.Recipe;
import org.jhandron.repository.RecipeRepository;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

public class MainFrame extends JFrame {
    private final RecipeRepository repository;
    private final RecipeListPanel listPanel;
    private final JTabbedPane editorTabs;
    private final Map<ObjectId, RecipeEditorPanel> openRecipeTabs = new HashMap<>();
    private final Map<RecipeEditorPanel, TabInfo> tabInfoLookup = new HashMap<>();
    private List<Recipe> allRecipes = new ArrayList<>();

    public MainFrame(boolean p_testEnvironment) {
        super("Recipe Notebook");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1100, 650));

        repository = new RecipeRepository(p_testEnvironment);
        listPanel = new RecipeListPanel();
        editorTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        setJMenuBar(buildMenuBar());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, editorTabs);
        listPanel.setMinimumSize(new Dimension(240, 200));
        editorTabs.setMinimumSize(new Dimension(520, 200));
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerSize(10);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        attachListListeners();
        loadAllRecipes(true);
        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem importItem = new JMenuItem("Import Recipes...");
        importItem.addActionListener(event -> importRecipes());
        JMenuItem exportItem = new JMenuItem("Export Recipes...");
        exportItem.addActionListener(event -> exportRecipes());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> System.exit(0));
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        JRadioButtonMenuItem lightModeItem = new JRadioButtonMenuItem("Light Mode");
        JRadioButtonMenuItem darkModeItem = new JRadioButtonMenuItem("Dark Mode");
        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightModeItem);
        themeGroup.add(darkModeItem);

        boolean darkActive = FlatLaf.isLafDark();
        lightModeItem.setSelected(!darkActive);
        darkModeItem.setSelected(darkActive);

        lightModeItem.addActionListener(event -> applyLookAndFeel(new FlatLightLaf()));
        darkModeItem.addActionListener(event -> applyLookAndFeel(new FlatDarkLaf()));

        viewMenu.add(lightModeItem);
        viewMenu.add(darkModeItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        return menuBar;
    }

    private void importRecipes() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Recipes");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines (*.json)", "json"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Import recipes from:\n" + file.getAbsolutePath()
                        + "\n\nExisting recipes with matching IDs will be updated.",
                "Confirm Import",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            int count = repository.importFromJson(file.toPath());
            loadAllRecipes(false);
            refreshEditorReferences();
            JOptionPane.showMessageDialog(this,
                    "Imported " + count + " recipes.",
                    "Import Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Unable to import recipes: " + ex.getMessage());
        }
    }

    private void exportRecipes() {
        if (allRecipes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There are no recipes available to export.",
                    "Nothing to Export",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Recipe> selected = promptForExportRecipes();
        if (selected.isEmpty()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Recipes");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines (*.json)", "json"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }
        try {
            int count = repository.exportToJson(file.toPath(), selected);
            JOptionPane.showMessageDialog(this,
                    "Exported " + count + " recipes to:\n" + file.getAbsolutePath(),
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Unable to export recipes: " + ex.getMessage());
        }
    }

    private List<Recipe> promptForExportRecipes() {
        JList<Recipe> recipeList = new JList<>(new Vector<>(allRecipes));
        recipeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        recipeList.setVisibleRowCount(12);
        JScrollPane scrollPane = new JScrollPane(recipeList);
        scrollPane.setPreferredSize(new Dimension(420, 240));

        int result = JOptionPane.showConfirmDialog(this,
                scrollPane,
                "Select Recipes to Export",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return List.of();
        }
        List<Recipe> selected = recipeList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select at least one recipe to export.",
                    "No Recipes Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return List.of();
        }
        return selected;
    }

    private void applyLookAndFeel(FlatLaf lookAndFeel) {
        FlatLaf.setup(lookAndFeel);
        FlatLaf.updateUI();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void attachListListeners() {
        listPanel.addFilterChangeListener(this::applyFilter);
        listPanel.addResetListener(e -> {
            listPanel.resetFilters();
        });
        listPanel.addNewRecipeListener(e -> startNewRecipe());
        listPanel.addSelectionListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    Recipe selected = listPanel.getSelectedRecipe();
                    if (selected != null) {
                        openRecipeTab(selected);
                    }
                }
            }
        });
    }

    private void startNewRecipe() {
        listPanel.clearSelection();
        Recipe newRecipe = new Recipe();
        openRecipeTab(newRecipe);
    }

    private void loadAllRecipes(boolean openNewTab) {
        try {
            allRecipes = repository.listAll();
            applyFilter();
            refreshEditorReferences();
            if (openNewTab) {
                if (!allRecipes.isEmpty()) {
                    SwingUtilities.invokeLater(() -> listPanel.clearSelection());
                }
                startNewRecipe();
            }
        } catch (Exception ex) {
            showError("Unable to load recipes: " + ex.getMessage());
        }
    }

    private void saveRecipe(RecipeEditorPanel panel, Recipe recipe) {
        try {
            Recipe saved = repository.save(recipe);
            loadAllRecipes(false);
            selectRecipe(saved.getId());
            panel.displayRecipe(saved, allRecipes);
            updateTabTitle(panel, saved);
            registerRecipeTab(panel, saved);
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

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void openRecipeTab(Recipe recipe) {
        if (recipe == null) {
            return;
        }
        ObjectId recipeId = recipe.getId();
        if (recipeId != null && openRecipeTabs.containsKey(recipeId)) {
            RecipeEditorPanel existing = openRecipeTabs.get(recipeId);
            editorTabs.setSelectedComponent(existing);
            return;
        }
        RecipeEditorPanel panel = new RecipeEditorPanel();
        panel.setRelatedSelector(this::openRelatedDialog);
        panel.setSaveListener(savedRecipe -> saveRecipe(panel, savedRecipe));
        panel.displayRecipe(recipe, allRecipes);

        String title = getTabTitle(recipe);
        editorTabs.addTab(title, panel);
        int index = editorTabs.indexOfComponent(panel);
        TabInfo tabInfo = buildTabHeader(title, () -> closeTab(panel));
        editorTabs.setTabComponentAt(index, tabInfo.container());
        tabInfoLookup.put(panel, tabInfo);
        editorTabs.setSelectedComponent(panel);

        registerRecipeTab(panel, recipe);
    }

    private void registerRecipeTab(RecipeEditorPanel panel, Recipe recipe) {
        ObjectId recipeId = recipe.getId();
        if (recipeId != null) {
            openRecipeTabs.put(recipeId, panel);
        }
    }

    private void closeTab(RecipeEditorPanel panel) {
        ObjectId recipeId = panel.getCurrentRecipeId();
        if (recipeId != null) {
            openRecipeTabs.remove(recipeId);
        }
        tabInfoLookup.remove(panel);
        editorTabs.remove(panel);
    }

    private String getTabTitle(Recipe recipe) {
        if (recipe == null || recipe.getId() == null) {
            return "[New Recipe]";
        }
        String name = recipe.getName();
        return name == null || name.isBlank() ? "[Untitled Recipe]" : name;
    }

    private void updateTabTitle(RecipeEditorPanel panel, Recipe recipe) {
        TabInfo info = tabInfoLookup.get(panel);
        if (info == null) {
            return;
        }
        String title = getTabTitle(recipe);
        info.titleLabel().setText(title);
    }

    private TabInfo buildTabHeader(String title, Runnable onClose) {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        container.setOpaque(false);

        JLabel titleLabel = new JLabel(title);

        JButton closeButton = new JButton("x");
        closeButton.setFocusable(false);
        closeButton.setMargin(new Insets(0, 4, 0, 4));
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        closeButton.addActionListener(e -> onClose.run());

        container.add(titleLabel);
        container.add(Box.createHorizontalStrut(2));
        container.add(closeButton);
        return new TabInfo(container, titleLabel);
    }

    private void refreshEditorReferences() {
        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            if (editorTabs.getComponentAt(i) instanceof RecipeEditorPanel panel) {
                panel.updateKnownRecipes(allRecipes);
            }
        }
    }

    private record TabInfo(JPanel container, JLabel titleLabel) {
    }
}
