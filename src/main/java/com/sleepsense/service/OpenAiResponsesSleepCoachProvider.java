package com.sleepsense.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** OpenAI Responses API adapter. Secret and request remain on the Java server. */
@Component
@ConditionalOnProperty(name = "ai.coach.provider", havingValue = "openai-responses")
public class OpenAiResponsesSleepCoachProvider implements SleepCoachProvider {
    private final ObjectMapper json;
    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final int timeoutSeconds;
    private final HttpClient client;

    public OpenAiResponsesSleepCoachProvider(ObjectMapper json,
            @Value("${ai.coach.api-key:}") String apiKey,
            @Value("${ai.coach.model:gpt-4.1-mini}") String model,
            @Value("${ai.coach.base-url:https://api.openai.com/v1/responses}") String endpoint,
            @Value("${ai.coach.timeout-seconds:12}") int timeoutSeconds) {
        this.json = json;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.endpoint = URI.create(endpoint);
        this.timeoutSeconds = Math.max(3, Math.min(timeoutSeconds, 30));
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        if (!"https".equalsIgnoreCase(this.endpoint.getScheme())) {
            throw new IllegalStateException("The AI provider endpoint must use HTTPS.");
        }
    }

    @Override
    public String generate(String instruction, String userInput) throws Exception {
        if (apiKey.isBlank()) throw new IllegalStateException("AI_API_KEY is not configured.");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", instruction);
        body.put("input", userInput);
        body.put("store", false);
        body.put("max_output_tokens", 500);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AI provider returned HTTP " + response.statusCode());
        }
        JsonNode root = json.readTree(response.body());
        StringBuilder output = new StringBuilder();
        JsonNode content = root.path("output");
        if (content.isArray()) {
            for (JsonNode item : content) {
                if (!"message".equals(item.path("type").asText())) continue;
                JsonNode parts = item.path("content");
                if (!parts.isArray()) continue;
                for (JsonNode part : parts) {
                    if ("output_text".equals(part.path("type").asText())) {
                        if (!output.isEmpty()) output.append('\n');
                        output.append(part.path("text").asText(""));
                    }
                }
            }
        }
        String result = output.toString().trim();
        if (result.isBlank()) throw new IllegalStateException("AI provider returned no text.");
        return result;
    }
}
