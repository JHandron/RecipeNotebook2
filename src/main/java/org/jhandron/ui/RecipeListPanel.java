package org.jhandron.ui;

import org.jhandron.model.Recipe;
import org.bson.types.ObjectId;
import org.jdesktop.swingx.JXTreeTable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.List;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jhandron.model.RecipeTreeTableModel;

public class RecipeListPanel extends JPanel {

    public enum FilterType {
        NAME,
        INGREDIENTS,
        TAGS
    }

    private final JTextField filterField;
    private final JRadioButton nameRadio;
    private final JRadioButton ingredientsRadio;
    private final JRadioButton tagsRadio;
    private final RecipeTreeTableModel treeTableModel;
    private final JXTreeTable recipeTreeTable;
    private final JButton newRecipeButton;
    private Runnable filterChangeListener;

    public RecipeListPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        filterField = new JTextField();
        filterField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Filter recipes");
        filterField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, Boolean.TRUE);
        filterField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new AlphaIcon(new FlatSVGIcon("icons/search.svg", 14, 14), 0.6f));
        nameRadio = new JRadioButton("Name");
        ingredientsRadio = new JRadioButton("Ingredients");
        tagsRadio = new JRadioButton("Tags");
        nameRadio.setSelected(true);
        ButtonGroup filterGroup = new ButtonGroup();
        filterGroup.add(nameRadio);
        filterGroup.add(ingredientsRadio);
        filterGroup.add(tagsRadio);

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Filter"));
        searchPanel.setAlignmentX(LEFT_ALIGNMENT);
        searchPanel.add(buildFilterTypePanel());
        searchPanel.add(Box.createVerticalStrut(6));
        searchPanel.add(buildFilterFieldPanel());

        treeTableModel = new RecipeTreeTableModel();
        recipeTreeTable = new JXTreeTable(treeTableModel);
        recipeTreeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recipeTreeTable.setRootVisible(false);
        recipeTreeTable.setShowsRootHandles(true);
        recipeTreeTable.setColumnControlVisible(false);

        JPanel newRecipePanel = new JPanel(new BorderLayout(6, 6));
        newRecipePanel.setBorder(BorderFactory.createTitledBorder("Start a new recipe"));
        newRecipePanel.setAlignmentX(LEFT_ALIGNMENT);
        newRecipeButton = new JButton("New Recipe");
        JLabel newRecipeHint = new JLabel("Add a new recipe to the notebook.");
        newRecipePanel.add(newRecipeHint, BorderLayout.CENTER);
        newRecipePanel.add(newRecipeButton, BorderLayout.EAST);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(newRecipePanel);
        header.add(Box.createVerticalStrut(8));
        header.add(searchPanel);

        add(header, BorderLayout.NORTH);
        add(buildListSection(), BorderLayout.CENTER);
        attachFilterListeners();
    }

    private JPanel buildFilterTypePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        panel.add(nameRadio);
        panel.add(ingredientsRadio);
        panel.add(tagsRadio);
        enforceFullWidth(panel);
        return panel;
    }

    private JPanel buildFilterFieldPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(filterField, BorderLayout.CENTER);
        enforceFullWidth(panel);
        return panel;
    }

    private void enforceFullWidth(JPanel panel) {
        Dimension preferred = panel.getPreferredSize();
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        panel.setAlignmentX(LEFT_ALIGNMENT);
    }

    public void updateList(List<Recipe> recipes) {
        treeTableModel.setRecipes(recipes);
        recipeTreeTable.expandAll();
    }

    public Recipe getSelectedRecipe() {
        int selectedRow = recipeTreeTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        TreePath path = recipeTreeTable.getPathForRow(selectedRow);
        if (path == null) {
            return null;
        }
        Object node = path.getLastPathComponent();
        return treeTableModel.recipeForNode(node);
    }

    public void clearSelection() {
        recipeTreeTable.clearSelection();
    }

    public String getFilterText() {
        return filterField.getText().trim();
    }

    public FilterType getSelectedFilterType() {
        if (ingredientsRadio.isSelected()) {
            return FilterType.INGREDIENTS;
        }
        if (tagsRadio.isSelected()) {
            return FilterType.TAGS;
        }
        return FilterType.NAME;
    }

    public void addFilterChangeListener(Runnable listener) {
        this.filterChangeListener = listener;
    }

    public void addNewRecipeListener(ActionListener listener) {
        newRecipeButton.addActionListener(listener);
    }

    public void addSelectionListener(MouseListener listener) {
        recipeTreeTable.addMouseListener(listener);
    }

    public void addSelectionChangeListener(ListSelectionListener listener) {
        recipeTreeTable.getSelectionModel().addListSelectionListener(listener);
    }

    public void resetFilters() {
        filterField.setText("");
        nameRadio.setSelected(true);
        notifyFilterChange();
    }

    public void selectRecipeById(ObjectId id) {
        if (id == null) {
            return;
        }
        for (int row = 0; row < recipeTreeTable.getRowCount(); row++) {
            TreePath path = recipeTreeTable.getPathForRow(row);
            if (path == null) {
                continue;
            }
            Object node = path.getLastPathComponent();
            Recipe recipe = treeTableModel.recipeForNode(node);
            if (recipe != null && id.equals(recipe.getId())) {
                recipeTreeTable.setRowSelectionInterval(row, row);
                recipeTreeTable.scrollRowToVisible(row);
                break;
            }
        }
    }

    private JPanel buildListSection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JScrollPane(recipeTreeTable), BorderLayout.CENTER);
        return panel;
    }

    private void attachFilterListeners() {
        filterField.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
            if (getFilterText().isBlank()) {
                nameRadio.setSelected(true);
            }
            notifyFilterChange();
        });
        ActionListener filterTypeListener = e -> notifyFilterChange();
        nameRadio.addActionListener(filterTypeListener);
        ingredientsRadio.addActionListener(filterTypeListener);
        tagsRadio.addActionListener(filterTypeListener);
    }

    private void notifyFilterChange() {
        if (filterChangeListener != null) {
            filterChangeListener.run();
        }
    }

    private static class AlphaIcon implements Icon {
        private final Icon delegate;
        private final float alpha;

        private AlphaIcon(Icon delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public int getIconWidth() {
            return delegate.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return delegate.getIconHeight();
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D graphics2d = (Graphics2D) g.create();
            graphics2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
            delegate.paintIcon(c, graphics2d, x, y);
            graphics2d.dispose();
        }
    }
}
