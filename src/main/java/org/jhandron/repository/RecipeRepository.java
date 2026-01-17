package org.jhandron.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.jhandron.db.MongoConnectionManager;
import org.jhandron.model.Recipe;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecipeRepository {
    private final MongoCollection<Document> collection;

    public RecipeRepository(boolean p_testEnvironment) {
        this.collection = MongoConnectionManager.getInstance().getRecipeCollection(p_testEnvironment);
    }

    public List<Recipe> listAll() {
        List<Recipe> recipes = new ArrayList<>();
        for (Document doc : collection.find().sort(Sorts.ascending("name"))) {
            recipes.add(Recipe.fromDocument(doc));
        }
        return recipes;
    }

    public Optional<Recipe> findById(ObjectId id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        if (doc == null) {
            return Optional.empty();
        }
        return Optional.of(Recipe.fromDocument(doc));
    }

    public List<Recipe> search(String nameQuery, List<String> tags, List<String> ingredientKeywords) {
        List<Bson> filters = new ArrayList<>();
        if (nameQuery != null && !nameQuery.isBlank()) {
            filters.add(Filters.regex("name", nameQuery, "i"));
        }
        if (tags != null && !tags.isEmpty()) {
            List<String> cleanedTags = tags.stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(String::trim)
                    .toList();
            if (!cleanedTags.isEmpty()) {
                filters.add(Filters.all("tags", cleanedTags));
            }
        }
        if (ingredientKeywords != null && !ingredientKeywords.isEmpty()) {
            List<String> cleanedKeywords = ingredientKeywords.stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .map(String::trim)
                    .toList();
            if (!cleanedKeywords.isEmpty()) {
                filters.add(Filters.or(cleanedKeywords.stream()
                        .map(keyword -> Filters.regex("ingredients", keyword, "i"))
                        .collect(Collectors.toList())));
            }
        }

        Bson finalFilter = filters.isEmpty() ? new Document() : Filters.and(filters);

        List<Recipe> results = new ArrayList<>();
        for (Document doc : collection.find(finalFilter).sort(Sorts.ascending("name"))) {
            results.add(Recipe.fromDocument(doc));
        }
        return results;
    }

    public Recipe save(Recipe recipe) {
        Objects.requireNonNull(recipe, "recipe cannot be null");
        if (recipe.getId() == null) {
            ObjectId newId = new ObjectId();
            recipe.setId(newId);
        }
        collection.replaceOne(Filters.eq("_id", recipe.getId()), recipe.toDocument(),
                new ReplaceOptions().upsert(true));
        return recipe;
    }

    public int exportToJson(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        JsonWriterSettings settings = JsonWriterSettings.builder()
                .outputMode(JsonMode.EXTENDED)
                .build();
        int count = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Document doc : collection.find().sort(Sorts.ascending("name"))) {
                writer.write(doc.toJson(settings));
                writer.newLine();
                count++;
            }
        }
        return count;
    }

    public int importFromJson(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Document doc = Document.parse(trimmed);
                ObjectId id = doc.getObjectId("_id");
                if (id == null) {
                    id = new ObjectId();
                    doc.put("_id", id);
                }
                collection.replaceOne(Filters.eq("_id", id), doc, new ReplaceOptions().upsert(true));
                count++;
            }
        }
        return count;
    }
}
