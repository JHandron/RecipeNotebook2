package org.jhandron.ui;

import org.jhandron.model.Recipe;
import org.bson.types.ObjectId;
import org.jdesktop.swingx.JXTreeTable;
import org.jhandron.model.RecipeTreeTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class RelatedRecipeDialog extends JDialog {
    private final RecipeTreeTableModel treeTableModel = new RecipeTreeTableModel();
    private final JXTreeTable recipeTreeTable = new JXTreeTable(treeTableModel);
    private List<Recipe> availableRecipes;
    private List<ObjectId> selectedIds = new ArrayList<>();

    public RelatedRecipeDialog(Frame owner, List<Recipe> availableRecipes, ObjectId currentId, List<ObjectId> preselected) {
        super(owner, "Select Related Recipes", true);
        this.availableRecipes = filterAvailable(availableRecipes, currentId);
        setLayout(new BorderLayout(8, 8));
        setSize(400, 450);
        setLocationRelativeTo(owner);
        add(buildFilterPanel(), BorderLayout.NORTH);
        recipeTreeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        recipeTreeTable.setRootVisible(false);
        recipeTreeTable.setShowsRootHandles(true);
        recipeTreeTable.setColumnControlVisible(false);
        add(new JScrollPane(recipeTreeTable), BorderLayout.CENTER);
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
            selectedIds = getSelectedRecipes().stream().map(Recipe::getId).collect(Collectors.toList());
            dispose();
        });
        panel.add(cancel);
        panel.add(save);
        return panel;
    }

    private void refreshList(String filter) {
        String normalized = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : availableRecipes) {
            String name = displayName(r);
            if (normalized.isEmpty() || name.toLowerCase(Locale.ROOT).contains(normalized)) {
                filtered.add(r);
            }
        }
        treeTableModel.setRecipes(filtered);
        recipeTreeTable.expandAll();
    }

    private void applySelection(List<ObjectId> preselected) {
        if (preselected == null || preselected.isEmpty()) {
            return;
        }
        Set<ObjectId> ids = Set.copyOf(preselected);
        recipeTreeTable.clearSelection();
        for (int row = 0; row < recipeTreeTable.getRowCount(); row++) {
            TreePath path = recipeTreeTable.getPathForRow(row);
            Object node = path == null ? null : path.getLastPathComponent();
            Recipe recipe = treeTableModel.recipeForNode(node);
            if (recipe != null && ids.contains(recipe.getId())) {
                recipeTreeTable.addRowSelectionInterval(row, row);
            }
        }
    }

    private List<Recipe> getSelectedRecipes() {
        List<Recipe> selected = new ArrayList<>();
        int[] rows = recipeTreeTable.getSelectedRows();
        for (int row : rows) {
            TreePath path = recipeTreeTable.getPathForRow(row);
            if (path == null) {
                continue;
            }
            Object node = path.getLastPathComponent();
            Recipe recipe = treeTableModel.recipeForNode(node);
            if (recipe != null) {
                selected.add(recipe);
            }
        }
        return selected;
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