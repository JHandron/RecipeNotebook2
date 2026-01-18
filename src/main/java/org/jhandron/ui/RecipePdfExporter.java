package org.jhandron.ui;

import org.jhandron.model.Recipe;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecipePdfExporter {
    private static final float MARGIN = 54f;
    private static final float TITLE_FONT_SIZE = 20f;
    private static final float SECTION_FONT_SIZE = 13f;
    private static final float BODY_FONT_SIZE = 11f;
    private static final float LINE_SPACING = 1.35f;
    private static final float SECTION_SPACING = 12f;
    private static final float LIST_INDENT = 14f;

    private RecipePdfExporter() {
    }

    public static void exportRecipe(Path path, Recipe recipe, List<String> relatedNames) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(recipe, "recipe cannot be null");
        List<String> ingredients = safeList(recipe.getIngredients());
        List<String> tags = safeList(recipe.getTags());
        List<String> related = safeList(relatedNames);

        try (PDDocument document = new PDDocument()) {
            PdfLayout layout = new PdfLayout(document);
            layout.newPage();

            String title = Objects.toString(recipe.getName(), "Untitled Recipe");
            layout.drawCenteredText(title, PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
            layout.moveDown(6f);
            layout.drawHorizontalRule();
            layout.moveDown(SECTION_SPACING);

            layout.addListSection("Ingredients", ingredients);
            layout.addListSection("Tags", tags);
            layout.addParagraphSection("Instructions", Objects.toString(recipe.getInstructions(), ""));
            layout.addListSection("Related Recipes", related);

            layout.close();
            document.save(path.toFile());
        }
    }

    private static List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                cleaned.add(value.trim());
            }
        }
        return cleaned;
    }

    private static final class PdfLayout implements AutoCloseable {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float yPosition;
        private float usableWidth;

        private PdfLayout(PDDocument document) {
            this.document = document;
        }

        private void newPage() throws IOException {
            closeStream();
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            yPosition = page.getMediaBox().getHeight() - MARGIN;
            usableWidth = page.getMediaBox().getWidth() - (MARGIN * 2);
        }

        private void closeStream() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private void ensureSpace(float heightNeeded) throws IOException {
            if (yPosition - heightNeeded < MARGIN) {
                newPage();
            }
        }

        private void moveDown(float amount) {
            yPosition -= amount;
        }

        private void drawCenteredText(String text, PDFont font, float fontSize) throws IOException {
            float textWidth = textWidth(text, font, fontSize);
            float startX = MARGIN + Math.max(0, (usableWidth - textWidth) / 2f);
            ensureSpace(fontSize + 4f);
            drawText(text, font, fontSize, startX, yPosition);
            moveDown(fontSize * LINE_SPACING);
        }

        private void drawHorizontalRule() throws IOException {
            ensureSpace(6f);
            contentStream.moveTo(MARGIN, yPosition);
            contentStream.lineTo(MARGIN + usableWidth, yPosition);
            contentStream.stroke();
            moveDown(6f);
        }

        private void addListSection(String title, List<String> items) throws IOException {
            addSectionHeader(title);
            List<String> values = items == null || items.isEmpty() ? List.of("None") : items;
            for (String value : values) {
                drawWrappedText("• " + value, PDType1Font.HELVETICA, BODY_FONT_SIZE, MARGIN + LIST_INDENT);
            }
            moveDown(SECTION_SPACING);
        }

        private void addParagraphSection(String title, String content) throws IOException {
            addSectionHeader(title);
            String safeContent = content == null ? "" : content.trim();
            if (safeContent.isEmpty()) {
                drawWrappedText("None", PDType1Font.HELVETICA, BODY_FONT_SIZE, MARGIN);
            } else {
                String[] paragraphs = safeContent.split("\\R");
                for (int i = 0; i < paragraphs.length; i++) {
                    String paragraph = paragraphs[i].trim();
                    if (!paragraph.isEmpty()) {
                        drawWrappedText(paragraph, PDType1Font.HELVETICA, BODY_FONT_SIZE, MARGIN);
                    }
                    if (i < paragraphs.length - 1) {
                        moveDown(BODY_FONT_SIZE * 0.6f);
                    }
                }
            }
            moveDown(SECTION_SPACING);
        }

        private void addSectionHeader(String title) throws IOException {
            ensureSpace(SECTION_FONT_SIZE + 8f);
            drawText(title, PDType1Font.HELVETICA_BOLD, SECTION_FONT_SIZE, MARGIN, yPosition);
            moveDown(SECTION_FONT_SIZE * LINE_SPACING);
        }

        private void drawWrappedText(String text, PDFont font, float fontSize, float x) throws IOException {
            List<String> lines = wrapText(text, font, fontSize, usableWidth - (x - MARGIN));
            for (String line : lines) {
                ensureSpace(fontSize * LINE_SPACING);
                drawText(line, font, fontSize, x, yPosition);
                moveDown(fontSize * LINE_SPACING);
            }
        }

        private void drawText(String text, PDFont font, float fontSize, float x, float y) throws IOException {
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
        }

        private float textWidth(String text, PDFont font, float fontSize) throws IOException {
            return font.getStringWidth(text) / 1000f * fontSize;
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (textWidth(candidate, font, fontSize) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (line.length() > 0) {
                        lines.add(line.toString());
                    }
                    line = new StringBuilder(word);
                }
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
            return lines;
        }

        @Override
        public void close() throws IOException {
            closeStream();
        }
    }
}
