package org.jhandron.ui;

import org.jhandron.model.Recipe;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.List;
import org.bson.types.ObjectId;

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
    private final DefaultListModel<Recipe> listModel;
    private final JList<Recipe> recipeJList;
    private final JButton resetButton;
    private final JButton newRecipeButton;
    private Runnable filterChangeListener;

    public RecipeListPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        filterField = new JTextField();
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
        searchPanel.add(buildFilterTypePanel());
        searchPanel.add(Box.createVerticalStrut(6));
        searchPanel.add(buildFilterFieldPanel());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resetButton = new JButton("Reset");
        buttonRow.add(resetButton);
        searchPanel.add(Box.createVerticalStrut(8));
        searchPanel.add(buttonRow);

        listModel = new DefaultListModel<>();
        recipeJList = new JList<>(listModel);
        recipeJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel newRecipePanel = new JPanel(new BorderLayout(6, 6));
        newRecipePanel.setBorder(BorderFactory.createTitledBorder("Start a new recipe"));
        newRecipeButton = new JButton("New Recipe");
        JLabel newRecipeHint = new JLabel("Add a new recipe to the notebook.");
        newRecipePanel.add(newRecipeHint, BorderLayout.CENTER);
        newRecipePanel.add(newRecipeButton, BorderLayout.EAST);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(searchPanel);
        header.add(Box.createVerticalStrut(8));
        header.add(newRecipePanel);

        add(header, BorderLayout.NORTH);
        add(buildListSection(), BorderLayout.CENTER);
        attachFilterListeners();
    }

    private JPanel buildFilterTypePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
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
        filterField.setText("");
        nameRadio.setSelected(true);
        updateResetButtonState();
        notifyFilterChange();
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

    private JPanel buildListSection() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JLabel header = new JLabel("Existing recipes (select to view or edit)");
        header.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(recipeJList), BorderLayout.CENTER);
        return panel;
    }

    private void attachFilterListeners() {
        resetButton.setEnabled(false);
        filterField.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
            updateResetButtonState();
            notifyFilterChange();
        });
        ActionListener filterTypeListener = e -> notifyFilterChange();
        nameRadio.addActionListener(filterTypeListener);
        ingredientsRadio.addActionListener(filterTypeListener);
        tagsRadio.addActionListener(filterTypeListener);
    }

    private void updateResetButtonState() {
        resetButton.setEnabled(!getFilterText().isBlank());
    }

    private void notifyFilterChange() {
        if (filterChangeListener != null) {
            filterChangeListener.run();
        }
    }
}
