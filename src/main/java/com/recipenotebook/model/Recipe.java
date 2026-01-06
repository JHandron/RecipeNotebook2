package com.recipenotebook.model;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Recipe {
    private ObjectId id;
    private String name;
    private List<String> ingredients;
    private List<String> tags;
    private String instructions;
    private List<ObjectId> relatedRecipeIds;

    public Recipe() {
        this.ingredients = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.relatedRecipeIds = new ArrayList<>();
    }

    public Recipe(ObjectId id, String name, List<String> ingredients, List<String> tags, String instructions,
                  List<ObjectId> relatedRecipeIds) {
        this.id = id;
        this.name = name;
        this.ingredients = new ArrayList<>(ingredients);
        this.tags = new ArrayList<>(tags);
        this.instructions = instructions;
        this.relatedRecipeIds = new ArrayList<>(relatedRecipeIds);
    }

    public static Recipe fromDocument(Document doc) {
        Recipe recipe = new Recipe();
        recipe.setId(doc.getObjectId("_id"));
        recipe.setName(doc.getString("name"));
        recipe.setIngredients(toStringList(doc.getList("ingredients", Object.class)));
        recipe.setTags(toStringList(doc.getList("tags", Object.class)));
        recipe.setInstructions(doc.getString("instructions"));
        recipe.setRelatedRecipeIds(toObjectIdList(doc.getList("relatedRecipes", Object.class)));
        return recipe;
    }

    public Document toDocument() {
        Document doc = new Document();
        if (id != null) {
            doc.put("_id", id);
        }
        doc.put("name", name);
        doc.put("ingredients", new ArrayList<>(ingredients));
        doc.put("tags", new ArrayList<>(tags));
        doc.put("instructions", instructions);
        doc.put("relatedRecipes", new ArrayList<>(relatedRecipeIds));
        return doc;
    }

    private static List<String> toStringList(List<Object> raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        return raw.stream().map(Object::toString).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<ObjectId> toObjectIdList(List<Object> raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        return raw.stream()
                .map(item -> item instanceof ObjectId ? (ObjectId) item : new ObjectId(Objects.toString(item)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = new ArrayList<>(ingredients);
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags);
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<ObjectId> getRelatedRecipeIds() {
        return Collections.unmodifiableList(relatedRecipeIds);
    }

    public void setRelatedRecipeIds(List<ObjectId> relatedRecipeIds) {
        this.relatedRecipeIds = new ArrayList<>(relatedRecipeIds);
    }

    @Override
    public String toString() {
        return name != null ? name : "(untitled recipe)";
    }
}
