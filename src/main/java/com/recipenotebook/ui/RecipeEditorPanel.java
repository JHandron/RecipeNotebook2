package com.recipenotebook.ui;

import com.recipenotebook.model.Recipe;
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
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

        ingredientsModel = new DefaultListModel<>();
        tagsModel = new DefaultListModel<>();
        relatedModel = new DefaultListModel<>();
        ingredientsList = new JList<>(ingredientsModel);
        tagsList = new JList<>(tagsModel);
        relatedList = new JList<>(relatedModel);

        instructionsArea = new JTextArea(8, 40);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildListsPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JLabel nameLabel = new JLabel("Recipe Name");
        nameLabel.setPreferredSize(new Dimension(120, 24));
        panel.add(nameLabel, BorderLayout.WEST);
        panel.add(nameField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildListsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));
        panel.add(buildListWithControls("Ingredients", ingredientsModel, ingredientsList));
        panel.add(buildListWithControls("Tags", tagsModel, tagsList));
        panel.add(buildRelatedPanel());
        return panel;
    }

    private JPanel buildListWithControls(String title, DefaultListModel<String> model, JList<String> list) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        JTextField input = new JTextField();
        input.addActionListener(e -> addListEntry(model, input));
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addListEntry(model, input));
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelected(model, list));
        controls.add(input);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(addButton);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(removeButton);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private void addListEntry(DefaultListModel<String> model, JTextField input) {
        String value = input.getText() == null ? "" : input.getText().trim();
        if (!value.isEmpty()) {
            model.addElement(value);
            input.setText("");
        }
    }

    private void removeSelected(DefaultListModel<String> model, JList<String> list) {
        int idx = list.getSelectedIndex();
        if (idx >= 0) {
            model.remove(idx);
        }
    }

    private JPanel buildRelatedPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Related Recipes"));

        panel.add(new JScrollPane(relatedList), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        JButton addButton = new JButton("Add Related");
        addButton.addActionListener(e -> addRelatedRecipes());
        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> {
            RelatedListItem selected = relatedList.getSelectedValue();
            if (selected != null) {
                relatedModel.removeElement(selected);
            }
        });
        controls.add(addButton);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(removeButton);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Instructions"));
        panel.add(new JScrollPane(instructionsArea), BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton saveButton = new JButton("Save Recipe");
        saveButton.addActionListener(e -> onSave());
        JButton resetButton = new JButton("Reset Form");
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

    public interface RelatedSelector {
        List<ObjectId> selectRelated(ObjectId currentId, List<ObjectId> alreadySelected);
    }
}
