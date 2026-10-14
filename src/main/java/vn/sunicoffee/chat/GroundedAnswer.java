package vn.sunicoffee.chat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Citations come from provider grounding metadata, never URLs invented in model text. */
final class GroundedAnswer {
    private GroundedAnswer() {}
    static ChatReply parse(JsonNode root) {
        var candidate = root.path("candidates").path(0);
        if (!candidate.path("finishReason").asText().equals("STOP")) throw new GeminiClient.Unavailable(GeminiClient.Failure.INVALID_RESPONSE);
        var text = new StringBuilder();
        for (var part : candidate.path("content").path("parts"))
            if (!part.path("thought").asBoolean(false) && part.path("text").isTextual()) text.append(part.path("text").asText());
        String answer = text.toString();
        if (answer.isBlank() || answer.length() > 6000) throw new GeminiClient.Unavailable(GeminiClient.Failure.INVALID_RESPONSE);
        var metadata = candidate.path("groundingMetadata");
        var chunks = metadata.path("groundingChunks");
        var sources = new LinkedHashMap<Integer, ChatReply.Citation>();
        var insertions = new TreeMap<Integer, LinkedHashSet<String>>(Comparator.reverseOrder());
        byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
        for (var support : metadata.path("groundingSupports")) {
            int end = support.path("segment").path("endIndex").asInt(-1);
            int start = support.path("segment").path("startIndex").asInt(0);
            String segment = support.path("segment").path("text").asText();
            // Gemini indices are UTF-8 byte offsets. Validate them before converting to Java indices.
            if (start < 0 || end <= start || end > bytes.length || segment.isBlank()
                || !new String(bytes, start, end - start, StandardCharsets.UTF_8).equals(segment)) continue;
            int position = new String(bytes, 0, end, StandardCharsets.UTF_8).length();
            var labels = new LinkedHashSet<String>();
            for (var index : support.path("groundingChunkIndices")) {
                int i = index.asInt(-1);
                if (i < 0 || i >= chunks.size()) continue;
                var web = chunks.path(i).path("web");
                String url = web.path("uri").asText(), title = web.path("title").asText();
                if (!safeUrl(url) || title.isBlank() || title.length() > 500) continue;
                String id = "W" + (i + 1);
                sources.putIfAbsent(i, new ChatReply.Citation(id, "[" + id + "] " + title, url));
                labels.add("[" + id + "]");
            }
            if (!labels.isEmpty()) insertions.computeIfAbsent(position, ignored -> new LinkedHashSet<>()).addAll(labels);
        }
        String suggestions = metadata.path("searchEntryPoint").path("renderedContent").asText();
        if (sources.isEmpty() || suggestions.isBlank() || suggestions.length() > 50_000)
            return ChatReply.of("INSUFFICIENT", "Mình chưa tìm được nguồn web có thể kiểm chứng cho câu hỏi này. Bạn có thể nêu cụ thể loại hạt hoặc dụng cụ đang dùng.");
        var cited = new StringBuilder(answer);
        insertions.forEach((position, labels) -> cited.insert(position, " " + String.join("", labels)));
        return new ChatReply("ANSWER", "Tham khảo từ web:\n" + cited, List.copyOf(sources.values()), suggestions);
    }
    private static boolean safeUrl(String value) {
        try {
            var uri = URI.create(value);
            String host = uri.getHost();
            return value.length() <= 4096 && "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                && host != null && host.contains(".") && !host.endsWith(".local") && !host.endsWith(".localhost")
                && !host.matches("[0-9.]+") && !host.contains(":");
        } catch (IllegalArgumentException ex) { return false; }
    }
}
