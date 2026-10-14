package vn.sunicoffee.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GroundedAnswerTest {
    final ObjectMapper mapper = new ObjectMapper();
    Map<String,Object> response(String uri, boolean support, String finish) {
        String text = "Cà phê có hương thơm.";
        return Map.of("candidates", List.of(Map.of("finishReason", finish,
            "content", Map.of("parts", List.of(Map.of("text", text))),
            "groundingMetadata", Map.of("searchEntryPoint", Map.of("renderedContent", "<div>Google Search</div>"),
                "groundingChunks", List.of(Map.of("web", Map.of("title", "Tài liệu", "uri", uri))),
                "groundingSupports", support ? List.of(Map.of("segment", Map.of("startIndex", 0, "endIndex", text.getBytes(StandardCharsets.UTF_8).length, "text", text), "groundingChunkIndices", List.of(0))) : List.of()))));
    }
    @Test void vietnameseUtf8OffsetsProduceCitationsFromMetadata() {
        var reply = GroundedAnswer.parse(mapper.valueToTree(response("https://example.org/coffee", true, "STOP")));
        assertThat(reply.status()).isEqualTo("ANSWER");
        assertThat(reply.answer()).contains("Cà phê có hương thơm. [W1]");
        assertThat(reply.sources()).extracting(ChatReply.Citation::url).containsExactly("https://example.org/coffee");
        assertThat(reply.searchSuggestionsHtml()).isEqualTo("<div>Google Search</div>");
    }
    @Test void absentSupportOrUnsafeUrlsDoNotCountAsVerifiedSources() {
        assertThat(GroundedAnswer.parse(mapper.valueToTree(response("https://example.org", false, "STOP"))).status()).isEqualTo("INSUFFICIENT");
        for (String url : List.of("javascript:alert(1)", "http://example.org", "https://localhost/test", "https://127.0.0.1/test", "https://user:pass@example.org"))
            assertThat(GroundedAnswer.parse(mapper.valueToTree(response(url, true, "STOP"))).status()).isEqualTo("INSUFFICIENT");
    }
    @Test void incompleteResponseIsRejected() {
        assertThatThrownBy(() -> GroundedAnswer.parse(mapper.valueToTree(response("https://example.org", true, "MAX_TOKENS"))))
            .isInstanceOf(GeminiClient.Unavailable.class);
    }
}
