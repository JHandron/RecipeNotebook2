## RecipeNotebook

Desktop recipe manager built with Java Swing and MongoDB.

### Requirements
- Java 17+
- MongoDB instance (defaults to `mongodb://localhost:27017`, database `recipe_notebook`)

Environment variables:
- `MONGODB_URI` — MongoDB connection string (optional)
- `MONGODB_DATABASE` — database name (optional)

### Build & Run
```bash
mvn clean package
java -jar target/recipenotebook-0.1.0.jar
```

### Features
- Create and edit recipes with name, ingredients, tags, instructions, and related recipe links.
- Browse all recipes or search by name, tags, or ingredient keywords.
- Modern FlatLaf look and feel.
- Prevents blank names/ingredients and blocks self-references in related recipes.
