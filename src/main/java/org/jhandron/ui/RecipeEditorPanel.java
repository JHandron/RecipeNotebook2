package org.jhandron.ui;

import org.jhandron.model.Recipe;
import org.bson.types.ObjectId;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class RecipeEditorPanel extends JPanel {
    private final JTextField nameField;
    private final DefaultListModel<String> ingredientsModel;
    private final DefaultListModel<String> tagsModel;
    private final DefaultListModel<RelatedListItem> relatedModel;
    private final JList<String> ingredientsList;
    private final JList<String> tagsList;
    private final JList<RelatedListItem> relatedList;
    private final JTextArea instructionsArea;
    private final JLabel modeBadge;
    private final JLabel modeDescription;
    private final JButton saveButton;
    private final JButton resetButton;
    private final JButton addRelatedButton;
    private final JButton removeRelatedButton;
    private ListControlGroup ingredientControls;
    private ListControlGroup tagControls;
    private Recipe currentRecipe;
    private List<Recipe> knownRecipes = new ArrayList<>();
    private Consumer<Recipe> saveListener;
    private Runnable resetListener;
    private RelatedSelector relatedSelector;
    private Map<ObjectId, String> recipeNameLookup = new HashMap<>();

    public RecipeEditorPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        nameField = new JTextField();
        nameField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateSaveButtonState());

        ingredientsModel = new DefaultListModel<>();
        tagsModel = new DefaultListModel<>();
        relatedModel = new DefaultListModel<>();
        ingredientsList = new JList<>(ingredientsModel);
        tagsList = new JList<>(tagsModel);
        relatedList = new JList<>(relatedModel);
        relatedList.addListSelectionListener(e -> updateRelatedActions());

        setCompactListRowHeight(ingredientsList);
        setCompactListRowHeight(tagsList);
        setCompactListRowHeight(relatedList);

        instructionsArea = new JTextArea(10, 40);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);

        modeBadge = new JLabel();
        modeBadge.setOpaque(true);
        modeBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        modeDescription = new JLabel();
        saveButton = new JButton("Save Recipe");
        resetButton = new JButton("Reset Form");
        addRelatedButton = new JButton("Add Related");
        removeRelatedButton = new JButton("Remove Selected");

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildListsPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        updateListControls();
        updateRelatedActions();
        updateSaveButtonState();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel stateRow = new JPanel(new BorderLayout(6, 6));
        modeBadge.setPreferredSize(new Dimension(180, 28));
        stateRow.add(modeBadge, BorderLayout.WEST);
        stateRow.add(modeDescription, BorderLayout.CENTER);
        panel.add(stateRow);

        panel.add(Box.createVerticalStrut(6));

        JPanel nameRow = new JPanel(new BorderLayout(6, 6));
        JLabel nameLabel = new JLabel("Recipe Name");
        nameLabel.setPreferredSize(new Dimension(120, 24));
        nameRow.add(nameLabel, BorderLayout.WEST);
        nameRow.add(nameField, BorderLayout.CENTER);
        panel.add(nameRow);
        return panel;
    }

    private JPanel buildListsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));
        ingredientControls = buildListWithControls("Ingredients", ingredientsModel, ingredientsList);
        tagControls = buildListWithControls("Tags", tagsModel, tagsList);
        panel.add(ingredientControls.panel());
        panel.add(tagControls.panel());
        panel.add(buildRelatedPanel());
        return panel;
    }

    private ListControlGroup buildListWithControls(String title, DefaultListModel<String> model, JList<String> list) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        JTextField input = new JTextField();
        input.addActionListener(e -> addListEntry(model, input));
        input.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateListControls());
        JButton addButton = new JButton("+");

        addButton.addActionListener(e -> addListEntry(model, input));
        JButton removeButton = new JButton("-");
        removeButton.addActionListener(e -> removeSelected(model, list));
        list.addListSelectionListener(e -> updateListControls());
        controls.add(input);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(addButton);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(removeButton);
        panel.add(controls, BorderLayout.SOUTH);
        return new ListControlGroup(panel, input, addButton, removeButton, list);
    }

    private void addListEntry(DefaultListModel<String> model, JTextField input) {
        String value = input.getText() == null ? "" : input.getText().trim();
        if (!value.isEmpty()) {
            model.addElement(value);
            input.setText("");
            updateListControls();
        }
    }

    private void removeSelected(DefaultListModel<String> model, JList<String> list) {
        List<String> selected = list.getSelectedValuesList();
        if (selected.isEmpty()) {
            return;
        }
        for (String s : selected) {
            model.removeElement(s);
        }
        updateListControls();
    }

    private JPanel buildRelatedPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Related Recipes"));

        panel.add(new JScrollPane(relatedList), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        addRelatedButton.addActionListener(e -> addRelatedRecipes());
        removeRelatedButton.addActionListener(e -> {
            RelatedListItem selected = relatedList.getSelectedValue();
            if (selected != null) {
                relatedModel.removeElement(selected);
            }
            updateRelatedActions();
            updateSaveButtonState();
        });
        controls.add(addRelatedButton);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(removeRelatedButton);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Instructions"));
        panel.add(new JScrollPane(instructionsArea), BorderLayout.CENTER);

        JPanel actions = new JPanel();
        saveButton.addActionListener(e -> onSave());
        resetButton.addActionListener(e -> resetForm());
        actions.add(saveButton);
        actions.add(resetButton);

        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void onSave() {
        try {
            Recipe recipe = buildRecipeFromFields();
            if (saveListener != null) {
                saveListener.accept(recipe);
            }
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void resetForm() {
        if (resetListener != null) {
            resetListener.run();
        } else if (currentRecipe != null) {
            displayRecipe(currentRecipe, knownRecipes);
        }
    }

    public void displayRecipe(Recipe recipe, List<Recipe> knownRecipes) {
        this.currentRecipe = recipe;
        this.knownRecipes = new ArrayList<>(knownRecipes);
        updateLookup(knownRecipes);
        nameField.setText(recipe.getName() == null ? "" : recipe.getName());

        ingredientsModel.clear();
        recipe.getIngredients().forEach(ingredientsModel::addElement);

        tagsModel.clear();
        recipe.getTags().forEach(tagsModel::addElement);

        instructionsArea.setText(recipe.getInstructions() == null ? "" : recipe.getInstructions());

        relatedModel.clear();
        for (ObjectId id : recipe.getRelatedRecipeIds()) {
            if (id != null) {
                relatedModel.addElement(toRelatedItem(id));
            }
        }
        updateModeIndicators();
        updateListControls();
        updateRelatedActions();
        updateSaveButtonState();
    }

    public void updateKnownRecipes(List<Recipe> knownRecipes) {
        this.knownRecipes = new ArrayList<>(knownRecipes);
        updateLookup(knownRecipes);
        List<ObjectId> selectedIds = relatedList.getSelectedValuesList().stream()
                .map(RelatedListItem::id)
                .toList();
        List<ObjectId> relatedIds = getRelatedIds();
        relatedModel.clear();
        for (ObjectId id : relatedIds) {
            relatedModel.addElement(toRelatedItem(id));
        }
        for (int i = 0; i < relatedModel.size(); i++) {
            if (selectedIds.contains(relatedModel.get(i).id())) {
                relatedList.addSelectionInterval(i, i);
            }
        }
        updateRelatedActions();
    }

    public ObjectId getCurrentRecipeId() {
        return currentRecipe != null ? currentRecipe.getId() : null;
    }

    private void updateLookup(List<Recipe> recipes) {
        recipeNameLookup.clear();
        for (Recipe r : recipes) {
            if (r.getId() != null) {
                recipeNameLookup.put(r.getId(), Objects.toString(r.getName(), r.getId().toHexString()));
            }
        }
    }

    private void addRelatedRecipes() {
        if (relatedSelector == null) {
            JOptionPane.showMessageDialog(this, "No related recipe selector available.", "Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<ObjectId> existing = getRelatedIds();
        ObjectId currentId = currentRecipe != null ? currentRecipe.getId() : null;
        List<ObjectId> selected = relatedSelector.selectRelated(currentId, existing);
        relatedModel.clear();
        for (ObjectId id : selected) {
            relatedModel.addElement(toRelatedItem(id));
        }
        updateRelatedActions();
        updateSaveButtonState();
    }

    private RelatedListItem toRelatedItem(ObjectId id) {
        String label = recipeNameLookup.getOrDefault(id, id.toHexString());
        return new RelatedListItem(id, label);
    }

    private Recipe buildRecipeFromFields() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalStateException("Recipe name is required.");
        }

        List<String> ingredients = collectList(ingredientsModel);
        if (ingredients.isEmpty()) {
            throw new IllegalStateException("Add at least one ingredient.");
        }

        List<String> tags = collectList(tagsModel);
        String instructions = instructionsArea.getText() == null ? "" : instructionsArea.getText().trim();
        List<ObjectId> relatedIds = getRelatedIds();

        if (currentRecipe != null && currentRecipe.getId() != null) {
            relatedIds = relatedIds.stream()
                    .filter(id -> !id.equals(currentRecipe.getId()))
                    .toList();
        }

        Recipe recipe = new Recipe();
        if (currentRecipe != null) {
            recipe.setId(currentRecipe.getId());
        }
        recipe.setName(name);
        recipe.setIngredients(ingredients);
        recipe.setTags(tags);
        recipe.setInstructions(instructions);
        recipe.setRelatedRecipeIds(relatedIds);
        return recipe;
    }

    private List<String> collectList(DefaultListModel<String> model) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            String value = model.get(i).trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private List<ObjectId> getRelatedIds() {
        List<ObjectId> ids = new ArrayList<>();
        for (int i = 0; i < relatedModel.size(); i++) {
            ids.add(relatedModel.get(i).id());
        }
        return ids;
    }

    private void updateModeIndicators() {
        boolean editingExisting = currentRecipe != null && currentRecipe.getId() != null;
        if (editingExisting) {
            modeBadge.setText("Editing existing recipe");
            modeBadge.setBackground(new Color(0xFFF4CC));
            modeDescription.setText("Changes will update the selected recipe. Use Reset to reload the latest saved version.");
        } else {
            modeBadge.setText("Creating new recipe");
            modeBadge.setBackground(new Color(0xE6F4EA));
            modeDescription.setText("Fill in the fields to add a new recipe. You can still relate it to other saved recipes.");
        }
    }

    private void updateListControls() {
        toggleListControls(ingredientControls, ingredientsModel);
        toggleListControls(tagControls, tagsModel);
        updateSaveButtonState();
    }

    private void toggleListControls(ListControlGroup controls, DefaultListModel<String> model) {
        if (controls == null) {
            return;
        }
        boolean hasInput = controls.input().getText() != null && !controls.input().getText().trim().isEmpty();
        controls.addButton().setEnabled(hasInput);
        controls.removeButton().setEnabled(!controls.list().isSelectionEmpty());
    }

    private void updateRelatedActions() {
        boolean hasOptions = knownRecipes.stream()
                .anyMatch(r -> r.getId() != null && (currentRecipe == null || !r.getId().equals(currentRecipe.getId())));
        addRelatedButton.setEnabled(hasOptions);
        addRelatedButton.setToolTipText(hasOptions ? "Choose existing recipes to relate to this one." : "No other saved recipes to relate yet.");
        removeRelatedButton.setEnabled(!relatedList.isSelectionEmpty());
    }

    private void updateSaveButtonState() {
        boolean hasName = nameField.getText() != null && !nameField.getText().trim().isEmpty();
        boolean hasIngredients = ingredientsModel.getSize() > 0;
        boolean hasTags = tagsModel.getSize() > 0;
        saveButton.setEnabled(hasName && hasIngredients && hasTags);
    }

    private void setCompactListRowHeight(JList<?> list) {
        int current = list.getFixedCellHeight();
        if (current <= 0 || current > 20) {
            list.setFixedCellHeight(20);
        }
    }

    public void setSaveListener(Consumer<Recipe> saveListener) {
        this.saveListener = saveListener;
    }

    public void setResetListener(Runnable resetListener) {
        this.resetListener = resetListener;
    }

    public void setRelatedSelector(RelatedSelector selector) {
        this.relatedSelector = selector;
    }

    private record RelatedListItem(ObjectId id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ListControlGroup(JPanel panel, JTextField input, JButton addButton, JButton removeButton, JList<String> list) {
    }

    public interface RelatedSelector {
        List<ObjectId> selectRelated(ObjectId currentId, List<ObjectId> alreadySelected);
    }
}
