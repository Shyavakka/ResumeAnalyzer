
package com.navadhiti.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navadhiti.resumeanalyzer.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ResumeAnalysisService {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String MODEL =
            "gemini-3.1-flash-lite";

    private static final int MAX_RETRIES = 3;

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ResumeAnalysisService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.mapper = new ObjectMapper();
    }

    public AnalysisResult analyze(String resumeText) {

        // Check API key
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResumeProcessingException(
                    "Gemini API key is missing. " +
                    "Please set the GEMINI_API_KEY environment variable."
            );
        }

        // Check resume text
        if (resumeText == null || resumeText.isBlank()) {
            throw new ResumeProcessingException(
                    "Resume text is empty. Please upload a valid PDF."
            );
        }

        String prompt = buildPrompt(resumeText);

        try {

            // Convert prompt to valid JSON string
            String promptJson = mapper.writeValueAsString(prompt);

            String requestBody = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": %s
                            }
                          ]
                        }
                      ],
                      "generationConfig": {
                        "temperature": 0.3,
                        "response_mime_type": "application/json"
                      }
                    }
                    """.formatted(promptJson);

            String url = API_URL + MODEL + ":generateContent";

            // Gemini API request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Retry for temporary errors
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                int status = response.statusCode();

                // -----------------------------
                // SUCCESS
                // -----------------------------
                if (status == 200) {
                    return parseResponse(response.body());
                }

                // -----------------------------
                // AUTHENTICATION ERROR
                // -----------------------------
                if (status == 401) {
                    throw new ResumeProcessingException(
                            "Gemini authentication failed (401). " +
                            "Please check your GEMINI_API_KEY."
                    );
                }

                // -----------------------------
                // FORBIDDEN
                // -----------------------------
                if (status == 403) {
                    throw new ResumeProcessingException(
                            "Gemini API access was forbidden (403). " +
                            "Please check your API key and Google AI Studio project settings."
                    );
                }

                // -----------------------------
                // RATE LIMIT
                // -----------------------------
                if (status == 429) {

                    if (isDailyQuotaExceeded(response.body())) {
                        throw new ResumeProcessingException(
                                "Gemini 3.1 Flash-Lite daily quota has been exceeded. " +
                                "Please try again after the quota resets."
                        );
                    }

                    long waitSeconds =
                            extractRetryDelay(response.body());

                    if (attempt < MAX_RETRIES) {

                        sleep(waitSeconds);

                        continue;
                    }

                    throw new ResumeProcessingException(
                            "Gemini API rate limit exceeded. " +
                            "Please try again later."
                    );
                }

                // -----------------------------
                // TEMPORARY GOOGLE ERROR
                // -----------------------------
                if (status == 500 ||
                    status == 502 ||
                    status == 503 ||
                    status == 504) {

                    if (attempt < MAX_RETRIES) {

                        long waitSeconds =
                                (long) Math.pow(2, attempt);

                        sleep(waitSeconds);

                        continue;
                    }

                    throw new ResumeProcessingException(
                            "Gemini service is temporarily unavailable. " +
                            "Please try again later."
                    );
                }

                // -----------------------------
                // OTHER ERRORS
                // -----------------------------
                throw new ResumeProcessingException(
                        "AI service returned an error " +
                        "(status " + status + "): " +
                        response.body()
                );
            }

            throw new ResumeProcessingException(
                    "Unable to get a response from Gemini."
            );

        } catch (ResumeProcessingException e) {

            throw e;

        } catch (Exception e) {

            throw new ResumeProcessingException(
                    "Failed to analyze resume using Gemini: " +
                    e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // Parse Gemini response
    // =========================================================

    private AnalysisResult parseResponse(String responseBody) {

        try {

            JsonNode root = mapper.readTree(responseBody);

            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new ResumeProcessingException(
                        "Gemini returned no candidates."
                );
            }

            JsonNode parts = candidates
                    .get(0)
                    .path("content")
                    .path("parts");

            if (!parts.isArray() || parts.isEmpty()) {
                throw new ResumeProcessingException(
                        "Gemini returned no text."
                );
            }

            String content = parts
                    .get(0)
                    .path("text")
                    .asText();

            if (content == null || content.isBlank()) {
                throw new ResumeProcessingException(
                        "Gemini returned an empty response."
                );
            }

            content = cleanJson(content);

            return mapper.readValue(
                    content,
                    AnalysisResult.class
            );

        } catch (ResumeProcessingException e) {

            throw e;

        } catch (Exception e) {

            throw new ResumeProcessingException(
                    "Failed to parse Gemini response: " +
                    e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // Clean JSON response
    // =========================================================

    private String cleanJson(String text) {

        text = text.trim();

        // Remove ```json
        if (text.startsWith("```json")) {
            text = text.substring(7).trim();
        }

        // Remove ```
        if (text.startsWith("```")) {
            text = text.substring(3).trim();
        }

        // Remove ending ```
        if (text.endsWith("```")) {
            text = text.substring(
                    0,
                    text.length() - 3
            ).trim();
        }

        // Find actual JSON object
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start >= 0 && end >= start) {
            return text.substring(start, end + 1);
        }

        return text;
    }

    // =========================================================
    // Check daily quota
    // =========================================================

    private boolean isDailyQuotaExceeded(String responseBody) {

        return responseBody.contains("PerDayPerProjectPerModel")
                || responseBody.contains("per_day")
                || responseBody.contains("daily");
    }

    // =========================================================
    // Extract retry delay
    // =========================================================

    private long extractRetryDelay(String responseBody) {

        try {

            JsonNode root = mapper.readTree(responseBody);

            JsonNode details = root
                    .path("error")
                    .path("details");

            if (details.isArray()) {

                for (JsonNode detail : details) {

                    if (detail.has("retryDelay")) {

                        String retryDelay =
                                detail.path("retryDelay").asText();

                        if (retryDelay.endsWith("s")) {

                            retryDelay = retryDelay.substring(
                                    0,
                                    retryDelay.length() - 1
                            );

                            return Math.max(
                                    1,
                                    Long.parseLong(retryDelay)
                            );
                        }
                    }
                }
            }

        } catch (Exception ignored) {
            // Use default delay
        }

        return 20;
    }

    // =========================================================
    // Sleep before retry
    // =========================================================

    private void sleep(long seconds) {

        try {

            Thread.sleep(seconds * 1000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new ResumeProcessingException(
                    "Retry interrupted.",
                    e
            );
        }
    }

    // =========================================================
    // Gemini prompt
    // =========================================================

    private String buildPrompt(String resumeText) {

        return """
                You are an expert resume analyzer.

                Analyze the following resume carefully.

                Return ONLY valid JSON with exactly these fields:

                {
                  "score": 0,
                  "profileSummary": "",
                  "keyStrengths": [],
                  "areasForImprovement": [],
                  "missingSkillsOrSections": [],
                  "suggestions": []
                }

                Requirements:

                1. score must be an integer from 0 to 100.

                2. profileSummary must contain 2-3 sentences.

                3. keyStrengths must be an array of strengths
                   found in the resume.

                4. areasForImprovement must be an array.

                5. missingSkillsOrSections must be an array.

                6. suggestions must be an array.

                7. Do not invent information that is not present
                   in the resume.

                8. Analyze the actual resume content.

                9. Return JSON only.

                10. Do not use markdown.

                11. Do not add explanations outside the JSON.

                Resume:
                ------------------------------

                %s

                ------------------------------

                Return the JSON now.
                """.formatted(resumeText);
    }
}
