package com.recipenotebook.ui;

import com.recipenotebook.model.Recipe;
import org.bson.types.ObjectId;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RelatedRecipeDialog extends JDialog {
    private final DefaultListModel<Recipe> listModel = new DefaultListModel<>();
    private final JList<Recipe> recipeJList = new JList<>(listModel);
    private List<Recipe> availableRecipes;
    private List<ObjectId> selectedIds = new ArrayList<>();

    public RelatedRecipeDialog(Frame owner, List<Recipe> availableRecipes, ObjectId currentId, List<ObjectId> preselected) {
        super(owner, "Select Related Recipes", true);
        this.availableRecipes = filterAvailable(availableRecipes, currentId);
        setLayout(new BorderLayout(8, 8));
        setSize(400, 450);
        setLocationRelativeTo(owner);
        add(buildFilterPanel(), BorderLayout.NORTH);
        recipeJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(new JScrollPane(recipeJList), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        refreshList("");
        applySelection(preselected);
    }

    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(new JLabel("Filter by name"), BorderLayout.WEST);
        JTextField filterField = new JTextField();
        filterField.getDocument().addDocumentListener((SimpleDocumentListener) e -> refreshList(filterField.getText()));
        panel.add(filterField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> {
            selectedIds = new ArrayList<>();
            dispose();
        });
        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            selectedIds = recipeJList.getSelectedValuesList().stream()
                    .map(Recipe::getId)
                    .collect(Collectors.toList());
            dispose();
        });
        panel.add(cancel);
        panel.add(save);
        return panel;
    }

    private void refreshList(String filter) {
        String normalized = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        listModel.clear();
        for (Recipe r : availableRecipes) {
            String name = displayName(r);
            if (normalized.isEmpty() || name.toLowerCase(Locale.ROOT).contains(normalized)) {
                listModel.addElement(r);
            }
        }
    }

    private void applySelection(List<ObjectId> preselected) {
        if (preselected == null || preselected.isEmpty()) {
            return;
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            Recipe recipe = listModel.get(i);
            if (preselected.contains(recipe.getId())) {
                indices.add(i);
            }
        }
        int[] idxArr = indices.stream().mapToInt(Integer::intValue).toArray();
        recipeJList.setSelectedIndices(idxArr);
    }

    private List<Recipe> filterAvailable(List<Recipe> recipes, ObjectId currentId) {
        return recipes.stream()
                .filter(r -> r.getId() != null)
                .filter(r -> currentId == null || !r.getId().equals(currentId))
                .sorted((a, b) -> displayName(a).compareToIgnoreCase(displayName(b)))
                .toList();
    }

    private String displayName(Recipe recipe) {
        return recipe.getName() == null ? "(untitled recipe)" : recipe.getName();
    }

    public List<ObjectId> getSelectedIds() {
        return selectedIds;
    }
}
