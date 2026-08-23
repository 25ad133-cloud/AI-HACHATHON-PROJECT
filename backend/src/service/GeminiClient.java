package service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class GeminiClient {

    private String apiKey;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public GeminiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        resolveApiKey();
    }

    private void resolveApiKey() {
        // 1. Try system env variable
        this.apiKey = System.getenv("GEMINI_API_KEY");
        if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
            System.out.println("Gemini API Key resolved from System Environment.");
            return;
        }

        // 2. Try backend root .env or project root .env
        if (loadKeyFromEnvFile(".env") || loadKeyFromEnvFile("backend/.env") || loadKeyFromEnvFile("../.env")) {
            return;
        }

        // 3. Try user profile directory .env
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            loadKeyFromEnvFile(userHome + File.separator + ".env");
        }
    }

    private boolean loadKeyFromEnvFile(String pathStr) {
        try {
            File f = new File(pathStr);
            if (f.exists() && f.isFile()) {
                List<String> lines = Files.readAllLines(Paths.get(pathStr));
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("GEMINI_API_KEY=")) {
                        this.apiKey = trimmed.substring("GEMINI_API_KEY=".length()).trim();
                        // Strip quotes if present
                        if (this.apiKey.startsWith("\"") && this.apiKey.endsWith("\"")) {
                            this.apiKey = this.apiKey.substring(1, this.apiKey.length() - 1);
                        } else if (this.apiKey.startsWith("'") && this.apiKey.endsWith("'")) {
                            this.apiKey = this.apiKey.substring(1, this.apiKey.length() - 1);
                        }
                        if (!this.apiKey.isEmpty()) {
                            System.out.println("Gemini API Key resolved from env file: " + f.getAbsolutePath());
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore error and continue searching
        }
        return false;
    }

    public boolean isAvailable() {
        return this.apiKey != null && !this.apiKey.trim().isEmpty();
    }

    /**
     * Translates and normalizes user input (Tamil/Tanglish/English) into basic English keywords.
     */
    public String normalizeQuery(String rawQuery) {
        if (!isAvailable()) return rawQuery;

        String prompt = "Normalize the following user query (which might be in English, Tamil script, or Tanglish) into clean, basic English keywords for searching in a policy manual. "
                + "Return ONLY the normalized keywords, nothing else.\n\nQuery: " + rawQuery;

        return callGemini(prompt);
    }

    /**
     * Formulates an answer using ONLY the retrieved context.
     */
    public String generateAnswer(String rawQuery, String context) {
        if (!isAvailable()) return null;

        String prompt = "Answer the user question ONLY using the provided context below. "
                + "If the context does not contain the answer, respond exactly with: I don't know. This information is not available in the provided documents. "
                + "Do not use any outside knowledge.\n\n"
                + "Context:\n" + context + "\n\n"
                + "User Question: " + rawQuery + "\n\n"
                + "Requirements:\n"
                + "- If the query was in Tamil (script) or specifically asks for Tamil, respond in Tamil.\n"
                + "- Otherwise, default to simple English.\n"
                + "- Do not explain or mention the source document name here.\n"
                + "- Answer ONLY using the facts from the context.";

        return callGemini(prompt);
    }

    private String callGemini(String promptText) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            // Build payload
            JsonObject payload = new JsonObject();
            JsonArray contentsArray = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray partsArray = new JsonArray();
            JsonObject partObj = new JsonObject();
            
            partObj.addProperty("text", promptText);
            partsArray.add(partObj);
            contentObj.add("parts", partsArray);
            contentsArray.add(contentObj);
            payload.add("contents", contentsArray);

            String requestBody = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(12))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content != null) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts != null && parts.size() > 0) {
                            return parts.get(0).getAsJsonObject().get("text").getAsString().trim();
                        }
                    }
                }
            } else {
                System.err.println("Gemini API call failed with status: " + response.statusCode() + " and body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Gemini client connection error: " + e.getMessage());
        }
        return null;
    }
}
