package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import database.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KnowledgeBaseService {

    public static class Chunk {
        public String source;
        public String text;
        public List<String> terms;
        
        public Chunk(String source, String text) {
            this.source = source;
            this.text = text.trim();
            this.terms = tokenize(text);
        }
    }

    private final List<Chunk> chunks = new ArrayList<>();
    private final Map<String, Double> idfs = new HashMap<>();
    private final double CONFIDENCE_THRESHOLD = 0.15;
    private final GeminiClient geminiClient = new GeminiClient();

    public KnowledgeBaseService() {
        loadKnowledgeBase();
    }

    private void loadKnowledgeBase() {
        File folder = new File("knowledge-base");
        if (!folder.exists() || !folder.isDirectory()) {
            folder = new File("../knowledge-base");
        }
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("knowledge-base folder not found!");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("No policy text files found in knowledge-base.");
            return;
        }

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath());
                // Split by double newline or single newline if it marks separate paragraphs
                String[] paragraphs = content.split("(?m)^\\s*$");
                for (String p : paragraphs) {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty()) {
                        chunks.add(new Chunk(file.getName(), trimmed));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading " + file.getName() + ": " + e.getMessage());
            }
        }

        computeIDFs();
        System.out.println("Loaded " + chunks.size() + " chunks from " + files.length + " knowledge base files.");
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        // Match both English words, Tamil characters, and numbers
        Pattern pattern = Pattern.compile("[\\p{L}\\p{N}]+");
        Matcher matcher = pattern.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private void computeIDFs() {
        int N = chunks.size();
        if (N == 0) return;

        Map<String, Integer> docCounts = new HashMap<>();
        for (Chunk chunk : chunks) {
            Set<String> uniqueTerms = new HashSet<>(chunk.terms);
            for (String term : uniqueTerms) {
                docCounts.put(term, docCounts.getOrDefault(term, 0) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : docCounts.entrySet()) {
            double idf = Math.log(1.0 + (double) N / entry.getValue());
            idfs.put(entry.getKey(), idf);
        }
    }

    public static class ChatResponse {
        public String answer;
        public boolean found;
        public String evidence;
        public String source;
        public double confidence;

        public ChatResponse(String answer, boolean found, String evidence, String source, double confidence) {
            this.answer = answer;
            this.found = found;
            this.evidence = evidence;
            this.source = source;
            this.confidence = confidence;
        }
    }

    public ChatResponse processQuery(String rawQuestion) {
        if (rawQuestion == null || rawQuestion.trim().isEmpty()) {
            return new ChatResponse("Question cannot be empty.", false, null, null, 0.0);
        }

        String question = rawQuestion.trim();
        String processedQuestion = question;

        // Check if Gemini API is configured for translation and answer generation
        boolean isGeminiAvailable = geminiClient.isAvailable();
        
        if (isGeminiAvailable) {
            try {
                // Step 1: Normalize query to English keywords for better keyword matching if query is Tamil or Tanglish
                String intentQuery = geminiClient.normalizeQuery(question);
                if (intentQuery != null && !intentQuery.trim().isEmpty()) {
                    processedQuestion = intentQuery;
                }
            } catch (Exception e) {
                System.err.println("Gemini query normalization failed: " + e.getMessage());
            }
        } else {
            // Apply offline translation/expansion for Tamil and Tanglish terms
            processedQuestion = translateTamilOrTanglish(question);
        }

        try {
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("debug_log.txt"), 
                "raw: " + question + " | processed: " + processedQuestion + "\n", 
                java.nio.charset.StandardCharsets.UTF_8, 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {}

        // Perform keyword-based search on the processed question
        List<String> queryTerms = tokenize(processedQuestion);
        Chunk bestChunk = null;
        double highestScore = 0.0;

        for (Chunk chunk : chunks) {
            // Topic matching: if the query contains a topic, the document MUST contain that topic.
            String qClean = question.toLowerCase(Locale.ROOT);
            String docClean = chunk.text.toLowerCase(Locale.ROOT);
            
            List<String> majorTopics = List.of("attendance", "library", "exam", "exams", "examination", "academic", "hostel");
            boolean hasTopicInQuery = false;
            boolean docMatchesTopic = false;
            for (String topic : majorTopics) {
                if (qClean.contains(topic)) {
                    hasTopicInQuery = true;
                    if (docClean.contains(topic)) {
                        docMatchesTopic = true;
                    }
                }
            }
            if (hasTopicInQuery && !docMatchesTopic) {
                continue; // Skip document if it doesn't match the query's topic
            }

            // Strict check: if the question contains words like 'fee' or 'capital' or 'france'
            // and the document chunk does NOT contain them, we skip the chunk!
            boolean strictFailed = false;
            List<String> unanswerableKeywords = List.of("fee", "fees", "capital", "france", "paris", "admission", "scholarship", "money", "price", "cost", "salary");
            for (String keyword : unanswerableKeywords) {
                if (qClean.contains(keyword) && !docClean.contains(keyword)) {
                    strictFailed = true;
                    break;
                }
            }
            if (strictFailed) {
                continue; // Skip this chunk as it does not contain the key query term
            }

            double score = calculateTFIDFScore(queryTerms, chunk);
            // Boost score if the raw question has literal substring matches with the chunk text
            double overlapBoost = calculateKeywordOverlap(processedQuestion, chunk.text);
            score += overlapBoost;

            if (score > highestScore) {
                highestScore = score;
                bestChunk = chunk;
            }
        }

        // Normalize confidence to [0.0 - 1.0] range for UI
        double confidence = Math.min(1.0, highestScore);

        if (bestChunk == null || confidence < CONFIDENCE_THRESHOLD) {
            // Write to database audit log
            logToAudit(question, "CHAT_BOT_NOT_FOUND", null, "No matching document found. Confidence: " + confidence);
            return new ChatResponse(
                "I don't know. This information is not available in the provided documents.",
                false,
                null,
                null,
                0.0
            );
        }

        // Write to audit log
        logToAudit(question, "CHAT_BOT_FOUND", bestChunk.source, "Found match. Confidence: " + confidence);

        // If Gemini is available, use it to frame the final answer using the retrieved chunk as context
        if (isGeminiAvailable) {
            try {
                String generatedAnswer = geminiClient.generateAnswer(question, bestChunk.text);
                if (generatedAnswer != null && !generatedAnswer.trim().isEmpty()) {
                    return new ChatResponse(
                        generatedAnswer,
                        true,
                        bestChunk.text,
                        bestChunk.source,
                        confidence
                    );
                }
            } catch (Exception e) {
                System.err.println("Gemini answer generation failed, falling back to raw chunk: " + e.getMessage());
            }
        }

        // Offline / Fallback mode: return the exact retrieved text chunk as the answer
        return new ChatResponse(
            bestChunk.text,
            true,
            bestChunk.text,
            bestChunk.source,
            confidence
        );
    }

    private double calculateTFIDFScore(List<String> queryTerms, Chunk chunk) {
        if (queryTerms.isEmpty()) return 0.0;

        double score = 0.0;
        Map<String, Integer> termCounts = new HashMap<>();
        for (String term : chunk.terms) {
            termCounts.put(term, termCounts.getOrDefault(term, 0) + 1);
        }

        for (String qTerm : queryTerms) {
            if (termCounts.containsKey(qTerm)) {
                double tf = (double) termCounts.get(qTerm) / chunk.terms.size();
                double idf = idfs.getOrDefault(qTerm, 0.0);
                score += tf * idf;
            }
        }
        return score;
    }

    private double calculateKeywordOverlap(String query, String documentText) {
        String qClean = query.toLowerCase(Locale.ROOT);
        String docClean = documentText.toLowerCase(Locale.ROOT);

        // Compute direct keyword intersections
        List<String> qWords = tokenize(qClean);
        if (qWords.isEmpty()) return 0.0;

        int matches = 0;
        for (String word : qWords) {
            if (docClean.contains(word)) {
                matches++;
            }
        }

        // Ratio of query words found in document
        double ratio = (double) matches / qWords.size();
        return ratio * 0.3; // Weight of direct overlap boost is max 0.3
    }

    private void logToAudit(String query, String action, String source, String details) {
        String sql = "INSERT INTO audit_logs (action, details) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, action);
            pstmt.setString(2, "Query: \"" + query + "\" | Source: " + (source != null ? source : "None") + " | Details: " + details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    private String translateTamilOrTanglish(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        
        // Tamil script mappings
        if (q.contains("வருகை") || q.contains("அட்டெண்டன்ஸ்")) q += " attendance";
        if (q.contains("நூலகம்") || q.contains("புத்தகம்") || q.contains("லைப்ரரி")) q += " library";
        if (q.contains("தேர்வு") || q.contains("எக்ஸாம்") || q.contains("பரீட்சை")) q += " exam";
        if (q.contains("விதிமுறை") || q.contains("ஒழுங்கு")) q += " rules regulations academic";
        if (q.contains("விடுதி") || q.contains("ஹாஸ்டல்")) q += " hostel";
        if (q.contains("திறக்கும்") || q.contains("நேரம்")) q += " open timings hours";
        
        // Tanglish mappings
        if (q.contains("evlo") || q.contains("evalavu") || q.contains("evvalavu")) q += " how much minimum";
        if (q.contains("eppo") || q.contains("eppothu")) q += " when open timings";
        
        return q;
    }
}
