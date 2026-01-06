package com.recipenotebook.ui;

import com.recipenotebook.model.Recipe;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

public class RecipeListPanel extends JPanel {
    private final JTextField nameField;
    private final JTextField tagsField;
    private final JTextField ingredientsField;
    private final DefaultListModel<Recipe> listModel;
    private final JList<Recipe> recipeJList;
    private final JButton searchButton;
    private final JButton resetButton;
    private final JButton newRecipeButton;

    public RecipeListPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        nameField = new JTextField();
        tagsField = new JTextField();
        ingredientsField = new JTextField();

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.add(labeledField("Name", nameField));
        searchPanel.add(Box.createVerticalStrut(6));
        searchPanel.add(labeledField("Tags (comma separated)", tagsField));
        searchPanel.add(Box.createVerticalStrut(6));
        searchPanel.add(labeledField("Ingredient keywords", ingredientsField));

        JPanel buttonRow = new JPanel();
        searchButton = new JButton("Search");
        resetButton = new JButton("Reset");
        newRecipeButton = new JButton("New Recipe");
        buttonRow.add(searchButton);
        buttonRow.add(resetButton);
        buttonRow.add(newRecipeButton);
        searchPanel.add(Box.createVerticalStrut(8));
        searchPanel.add(buttonRow);

        listModel = new DefaultListModel<>();
        recipeJList = new JList<>(listModel);
        recipeJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(recipeJList), BorderLayout.CENTER);
    }

    private JPanel labeledField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(160, 24));
        panel.add(jLabel, BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    public void updateList(List<Recipe> recipes) {
        listModel.clear();
        for (Recipe recipe : recipes) {
            listModel.addElement(recipe);
        }
    }

    public Recipe getSelectedRecipe() {
        return recipeJList.getSelectedValue();
    }

    public void clearSelection() {
        recipeJList.clearSelection();
    }

    public String getNameQuery() {
        return nameField.getText().trim();
    }

    public List<String> getTagFilters() {
        String text = tagsField.getText();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public List<String> getIngredientKeywords() {
        String text = ingredientsField.getText();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void addSearchListener(ActionListener listener) {
        searchButton.addActionListener(listener);
    }

    public void addResetListener(ActionListener listener) {
        resetButton.addActionListener(listener);
    }

    public void addNewRecipeListener(ActionListener listener) {
        newRecipeButton.addActionListener(listener);
    }

    public void addSelectionListener(java.awt.event.MouseListener listener) {
        recipeJList.addMouseListener(listener);
    }

    public void addSelectionChangeListener(javax.swing.event.ListSelectionListener listener) {
        recipeJList.addListSelectionListener(listener);
    }

    public void resetFilters() {
        nameField.setText("");
        tagsField.setText("");
        ingredientsField.setText("");
    }

    public void selectRecipeById(ObjectId id) {
        if (id == null) {
            return;
        }
        for (int i = 0; i < listModel.size(); i++) {
            Recipe recipe = listModel.get(i);
            if (id.equals(recipe.getId())) {
                recipeJList.setSelectedIndex(i);
                recipeJList.ensureIndexIsVisible(i);
                break;
            }
        }
    }
}
