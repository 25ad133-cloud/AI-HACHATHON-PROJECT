package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.KnowledgeBaseService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ChatHandler implements HttpHandler {

    private final KnowledgeBaseService kbService;
    private final Gson gson = new Gson();

    public ChatHandler(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. Enable CORS Headers on every response
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // 2. Handle CORS Preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // 3. Reject non-POST requests
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            // 4. Read Request Body
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
            StringBuilder bodyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
            String requestBody = bodyBuilder.toString();

            if (requestBody.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, new KnowledgeBaseService.ChatResponse(
                        "I don't know. Request body is empty.", false, null, null, 0.0
                ));
                return;
            }

            // 5. Parse JSON
            JsonObject jsonRequest;
            try {
                jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, new KnowledgeBaseService.ChatResponse(
                        "I don't know. Invalid JSON payload.", false, null, null, 0.0
                ));
                return;
            }

            if (!jsonRequest.has("question")) {
                sendJsonResponse(exchange, 400, new KnowledgeBaseService.ChatResponse(
                        "I don't know. Question key is missing.", false, null, null, 0.0
                ));
                return;
            }

            String question = jsonRequest.get("question").getAsString();
            System.out.println("DEBUG - Received Question: " + question);

            // 6. Validation: Reject empty or oversized questions (max 1000 characters)
            if (question == null || question.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, new KnowledgeBaseService.ChatResponse(
                        "I don't know. Question cannot be empty.", false, null, null, 0.0
                ));
                return;
            }

            if (question.length() > 1000) {
                sendJsonResponse(exchange, 400, new KnowledgeBaseService.ChatResponse(
                        "I don't know. Question is too long (maximum 1000 characters).", false, null, null, 0.0
                ));
                return;
            }

            // 7. Process Query
            KnowledgeBaseService.ChatResponse response = kbService.processQuery(question);

            // 8. Send Response
            sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, new KnowledgeBaseService.ChatResponse(
                    "Internal server error occurred.", false, null, null, 0.0
            ));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        String jsonResponse = gson.toJson(responseObj);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
