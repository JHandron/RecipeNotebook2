package org.jhandron.ui;

import org.jhandron.model.Recipe;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import java.util.ArrayList;
import java.util.List;

public class RecipeTreeTableModel extends AbstractTreeTableModel {
    private final RootNode rootNode = new RootNode();

    public RecipeTreeTableModel() {
        super(new RootNode());
        this.root = rootNode;
    }

    public void setRecipes(List<Recipe> recipes) {
        rootNode.children = recipes.stream().map(RecipeNode::new).toList();
        modelSupport.fireNewRoot();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int column) {
        return "Recipe";
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (node instanceof RecipeNode recipeNode) {
            return recipeNode.recipe();
        }
        return "Recipes";
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof RootNode root) {
            return root.children.get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof RootNode root) {
            return root.children.size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof RootNode root) {
            return root.children.indexOf(child);
        }
        return -1;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof RecipeNode;
    }

    public Recipe recipeForNode(Object node) {
        if (node instanceof RecipeNode recipeNode) {
            return recipeNode.recipe();
        }
        return null;
    }

    private static class RootNode {
        private List<RecipeNode> children = new ArrayList<>();
    }

    private record RecipeNode(Recipe recipe) {
        @Override
        public String toString() {
            String name = recipe.getName();
            return name == null ? "(untitled recipe)" : name;
        }
    }
}
