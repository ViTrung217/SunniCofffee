package vn.sunicoffee.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiClient {
    public record Answer(String status, String answer, List<String> knowledgeIds) {}
    public static class Unavailable extends RuntimeException {
        public Unavailable() { super("Gemini unavailable"); }
    }
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final String instructions;
    private final Semaphore capacity = new Semaphore(4);

    @Autowired
    public GeminiClient(ObjectMapper mapper, @Value("${app.chat.enabled:false}") boolean enabled,
            @Value("${app.chat.gemini-api-key:}") String key,
            @Value("${app.chat.model:gemini-3.1-flash-lite}") String model) throws java.io.IOException {
        this(mapper, enabled, key, model, builder());
    }
    GeminiClient(ObjectMapper mapper, boolean enabled, String key, String model, RestClient.Builder builder) throws java.io.IOException {
        if (!model.matches("[a-zA-Z0-9._-]{1,100}")) throw new IllegalArgumentException("Invalid Gemini model name");
        this.mapper = mapper; this.enabled = enabled; this.apiKey = key.strip(); this.model = model;
        client = builder.baseUrl("https://generativelanguage.googleapis.com").build();
        instructions = new ClassPathResource("chat/system-prompt.txt").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    private static RestClient.Builder builder() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory);
    }
    public boolean ready() { return enabled && !apiKey.isBlank(); }
    public Answer answer(String question, List<KnowledgeBase.Entry> context) {
        if (!ready() || !capacity.tryAcquire()) throw new Unavailable();
        try {
            var schema = Map.of("type", "object", "properties", Map.of(
                "status", Map.of("type", "string", "enum", List.of("ANSWER", "CLARIFY", "OUT_OF_SCOPE", "INSUFFICIENT")),
                "answer", Map.of("type", "string"),
                "knowledgeIds", Map.of("type", "array", "items", Map.of("type", "string"))),
                "required", List.of("status", "answer", "knowledgeIds"), "additionalProperties", false);
            String input = mapper.writeValueAsString(Map.of("question", question, "knowledge", context));
            var body = Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", instructions))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", input)))),
                "generationConfig", Map.of("maxOutputTokens", 2048,
                    "responseFormat", Map.of("text", Map.of("mimeType", "application/json", "schema", schema))));
            String raw = client.post().uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(String.class);
            if (raw == null || raw.length() > 100_000) throw new Unavailable();
            var candidate = mapper.readTree(raw).path("candidates").path(0);
            if (!candidate.path("finishReason").asText().equals("STOP")) throw new Unavailable();
            var text = new StringBuilder();
            for (var part : candidate.path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false) && part.path("text").isTextual()) text.append(part.path("text").asText());
            }
            Answer result = mapper.readValue(text.toString(), Answer.class);
            if (result.answer() == null || result.answer().isBlank() || result.answer().length() > 6000 || result.knowledgeIds() == null
                    || !Set.of("ANSWER", "CLARIFY", "OUT_OF_SCOPE", "INSUFFICIENT").contains(result.status())) throw new Unavailable();
            Set<String> allowed = new HashSet<>(); context.forEach(e -> allowed.add(e.id()));
            if (result.knowledgeIds().size() > context.size() || !allowed.containsAll(result.knowledgeIds())
                    || (result.status().equals("ANSWER") && result.knowledgeIds().isEmpty())) throw new Unavailable();
            return result;
        } catch (Exception ex) {
            // Do not log raw prompts, responses, headers or upstream exception messages (may contain secrets).
            throw new Unavailable();
        } finally { capacity.release(); }
    }
}
