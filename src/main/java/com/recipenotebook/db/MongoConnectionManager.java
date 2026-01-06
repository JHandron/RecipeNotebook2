package com.recipenotebook.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Objects;

public class MongoConnectionManager {
    private static MongoConnectionManager instance;
    private final MongoClient client;
    private final MongoDatabase database;

    private MongoConnectionManager() {
        String uri = Objects.requireNonNullElse(System.getenv("MONGODB_URI"), "mongodb://localhost:27017");
        String databaseName = Objects.requireNonNullElse(System.getenv("MONGODB_DATABASE"), "recipe_notebook");

        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(databaseName);
    }

    public static synchronized MongoConnectionManager getInstance() {
        if (instance == null) {
            instance = new MongoConnectionManager();
        }
        return instance;
    }

    public MongoCollection<Document> getRecipeCollection() {
        return database.getCollection("recipes");
    }

    public void close() {
        client.close();
    }
}
