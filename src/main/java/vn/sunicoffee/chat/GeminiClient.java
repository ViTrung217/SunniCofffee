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
    public enum Failure { NOT_CONFIGURED, BUSY, INVALID_REQUEST, KEY_REJECTED, MODEL_UNAVAILABLE, QUOTA_EXCEEDED, NETWORK, INVALID_RESPONSE, PROVIDER_ERROR }
    public static class Unavailable extends RuntimeException {
        private final Failure failure;
        public Unavailable() { this(Failure.PROVIDER_ERROR); }
        public Unavailable(Failure failure) { super("Gemini unavailable"); this.failure = failure; }
        public Failure failure() { return failure; }
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeminiClient.class);
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
        factory.setReadTimeout(Duration.ofSeconds(25));
        return RestClient.builder().requestFactory(factory);
    }
    public boolean ready() { return enabled && !apiKey.isBlank(); }
    public Answer answer(String question, List<KnowledgeBase.Entry> context) {
        return answer(question, context, List.of());
    }
    public Answer answer(String question, List<KnowledgeBase.Entry> context, List<ChatConversation.Turn> history) {
        if (!ready()) throw new Unavailable(Failure.NOT_CONFIGURED);
        if (!capacity.tryAcquire()) throw new Unavailable(Failure.BUSY);
        try {
            var schema = Map.of("type", "object", "properties", Map.of(
                "status", Map.of("type", "string", "enum", List.of("ANSWER", "CLARIFY", "OUT_OF_SCOPE", "INSUFFICIENT")),
                "answer", Map.of("type", "string"),
                "knowledgeIds", Map.of("type", "array", "items", Map.of("type", "string", "enum", context.stream().map(KnowledgeBase.Entry::id).distinct().toList()))),
                "required", List.of("status", "answer", "knowledgeIds"), "additionalProperties", false);
            String input = mapper.writeValueAsString(Map.of("question", question, "history", history,
                "knownNeeds", AdviceNeeds.from(history, question), "knowledge", context));
            var body = Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", instructions))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", input)))),
                "generationConfig", Map.of("maxOutputTokens", 2048,
                    "responseMimeType", "application/json", "responseJsonSchema", schema));
            String raw = generate(body);
            if (raw == null || raw.length() > 100_000) throw new Unavailable(Failure.INVALID_RESPONSE);
            var candidate = mapper.readTree(raw).path("candidates").path(0);
            if (!candidate.path("finishReason").asText().equals("STOP")) {
                log.warn("Gemini response rejected: reason=INCOMPLETE");
                throw new Unavailable(Failure.INVALID_RESPONSE);
            }
            var text = new StringBuilder();
            for (var part : candidate.path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false) && part.path("text").isTextual()) text.append(part.path("text").asText());
            }
            Answer result = mapper.readValue(text.toString(), Answer.class);
            if (result.answer() == null || result.answer().isBlank() || result.answer().length() > 6000 || result.knowledgeIds() == null
                    || !Set.of("ANSWER", "CLARIFY", "OUT_OF_SCOPE", "INSUFFICIENT").contains(result.status())) throw new Unavailable(Failure.INVALID_RESPONSE);
            Set<String> allowed = new HashSet<>(); context.forEach(e -> allowed.add(e.id()));
            if (result.knowledgeIds().size() > context.size() || !allowed.containsAll(result.knowledgeIds())) {
                log.warn("Gemini response rejected: reason=UNKNOWN_SOURCE");
                throw new Unavailable(Failure.INVALID_RESPONSE);
            }
            if (result.status().equals("ANSWER") && result.knowledgeIds().isEmpty()) {
                log.warn("Gemini response rejected: reason=MISSING_SOURCE");
                throw new Unavailable(Failure.INVALID_RESPONSE);
            }
            return result;
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            Failure failure = switch (status) {
                case 400 -> ex.getResponseBodyAsString().contains("API_KEY_INVALID") ? Failure.KEY_REJECTED : Failure.INVALID_REQUEST;
                case 401, 403 -> Failure.KEY_REJECTED;
                case 404 -> Failure.MODEL_UNAVAILABLE;
                case 429 -> Failure.QUOTA_EXCEEDED;
                case 503 -> Failure.BUSY;
                default -> Failure.PROVIDER_ERROR;
            };
            // Only our fixed codes and HTTP status: no upstream body, prompt, headers or exception.
            log.warn("Gemini request failed: code={}, http={}", failure, status);
            throw new Unavailable(failure);
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            log.warn("Gemini request failed: code=NETWORK");
            throw new Unavailable(Failure.NETWORK);
        } catch (Unavailable ex) {
            log.warn("Gemini request failed: code={}", ex.failure());
            throw ex;
        } catch (Exception ex) {
            log.warn("Gemini request failed: code=INVALID_RESPONSE");
            throw new Unavailable(Failure.INVALID_RESPONSE);
        } finally { capacity.release(); }
    }

    /** A quick 503 may be retried once; never retry quota, credentials or read timeouts. */
    private String generate(Object body) {
        long start=System.nanoTime();
        for (int attempt=0; ; attempt++) {
            try {
                onRequestAttempt();
                return client.post().uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(String.class);
            } catch (org.springframework.web.client.RestClientResponseException ex) {
                if (ex.getStatusCode().value()!=503 || attempt!=0 || System.nanoTime()-start>=Duration.ofSeconds(5).toNanos()) throw ex;
                log.info("Gemini retry: reason=BUSY, attempt=2");
                try { Thread.sleep(250); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new Unavailable(Failure.NETWORK);
                }
            }
        }
    }

    /** No-op observer used by the isolated evaluation runner; never contains request data. */
    protected void onRequestAttempt() {}
}
