package vn.sunicoffee.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiClientTest {
    final ObjectMapper mapper = new ObjectMapper();
    MockRestServiceServer server;
    GeminiClient client;
    final List<KnowledgeBase.Entry> context = List.of(new KnowledgeBase.Entry("SP02", "Dung tích", "80 ml", List.of("SRC01"), "Tham khảo"));
    @BeforeEach void setup() throws Exception {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiClient(mapper, true, "test-key-not-real", "gemini-3.1-flash-lite", builder);
    }
    String response(String status, List<String> ids, String finish) throws Exception {
        String answer = mapper.writeValueAsString(Map.of("status", status, "answer", "Nội dung kiểm thử", "knowledgeIds", ids));
        return mapper.writeValueAsString(Map.of("candidates", List.of(Map.of("finishReason", finish,
            "content", Map.of("parts", List.of(Map.of("text", answer)))))));
    }
    @Test void usesHeaderAndStructuredOutputWithBoundedContext() throws Exception {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("x-goog-api-key", "test-key-not-real"))
            .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
            .andExpect(jsonPath("$.generationConfig.responseJsonSchema.required[0]").value("status"))
            .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(2048))
            .andExpect(jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.containsString("SP02")))
            .andRespond(withSuccess(response("ANSWER", List.of("SP02"), "STOP"), MediaType.APPLICATION_JSON));
        assertThat(client.answer("Bình chứa bao nhiêu?", context).knowledgeIds()).containsExactly("SP02");
        server.verify();
    }
    @Test void rejectsFabricatedCitation() throws Exception {
        server.expect(anything()).andRespond(withSuccess(response("ANSWER", List.of("NOT-A-SOURCE"), "STOP"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.answer("Câu hỏi", context)).isInstanceOf(GeminiClient.Unavailable.class);
        server.verify();
    }
    @Test void rejectsUncitedFactualAnswer() throws Exception {
        server.expect(anything()).andRespond(withSuccess(response("ANSWER", List.of(), "STOP"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.answer("Câu hỏi", context)).isInstanceOf(GeminiClient.Unavailable.class);
    }
    @Test void rejectsTruncatedAnswer() throws Exception {
        server.expect(anything()).andRespond(withSuccess(response("ANSWER", List.of("SP02"), "MAX_TOKENS"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.answer("Câu hỏi", context)).isInstanceOf(GeminiClient.Unavailable.class);
    }
    @Test void quotaFailureDoesNotRetryOrExposeRawError() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("test-key-not-real upstream detail"));
        assertThatThrownBy(() -> client.answer("Câu hỏi", context)).isInstanceOf(GeminiClient.Unavailable.class)
            .hasMessage("Gemini unavailable").hasNoCause();
        server.verify();
    }
    @Test void missingKeyMakesNoRequest() throws Exception {
        var disabled = new GeminiClient(mapper, true, "", "gemini-3.1-flash-lite", RestClient.builder());
        assertThat(disabled.ready()).isFalse();
        assertThatThrownBy(() -> disabled.answer("Câu hỏi", context)).isInstanceOf(GeminiClient.Unavailable.class);
        server.verify();
    }

    @Test void reportsSafeFailureCategories() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.FORBIDDEN).body("secret upstream text"));
        assertThatThrownBy(() -> client.answer("Câu hỏi", context))
            .isInstanceOfSatisfying(GeminiClient.Unavailable.class, ex -> {
                assertThat(ex.failure()).isEqualTo(GeminiClient.Failure.KEY_REJECTED);
                assertThat(ex.getMessage()).doesNotContain("secret");
                assertThat(ex.getCause()).isNull();
            });
    }
    @Test void malformedResponseDoesNotLeakOrConsumeCapacityPermanently() throws Exception {
        for (int i=0; i<5; i++) server.expect(anything()).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess(response("ANSWER", List.of("SP02"), "STOP"), MediaType.APPLICATION_JSON));
        for (int i=0; i<5; i++) assertThatThrownBy(() -> client.answer("Câu hỏi", context))
            .isInstanceOfSatisfying(GeminiClient.Unavailable.class, ex -> assertThat(ex.failure()).isEqualTo(GeminiClient.Failure.INVALID_RESPONSE));
        assertThat(client.answer("Câu hỏi", context).status()).isEqualTo("ANSWER");
        server.verify();
    }
    @Test void refusalCanHaveNoCitations() throws Exception {
        server.expect(anything()).andRespond(withSuccess(response("OUT_OF_SCOPE", List.of(), "STOP"), MediaType.APPLICATION_JSON));
        assertThat(client.answer("Câu hỏi", context).status()).isEqualTo("OUT_OF_SCOPE");
    }
    @Test void serializesPreviousNeedsAlongsideCurrentQuestion() throws Exception {
        server.expect(anything()).andExpect(jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.allOf(
            org.hamcrest.Matchers.containsString("history"), org.hamcrest.Matchers.containsString("2 triệu"), org.hamcrest.Matchers.containsString("arabica"))))
            .andRespond(withSuccess(response("ANSWER", List.of("SP02"), "STOP"), MediaType.APPLICATION_JSON));
        client.answer("arabica", context, List.of(new ChatConversation.Turn("pha máy, 2 người, 2 triệu", "Tư vấn trước")));
        server.verify();
    }

    @Test void persistent503StopsAfterOneRetry() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.answer("Câu hỏi", context)).isInstanceOfSatisfying(GeminiClient.Unavailable.class,
            ex -> assertThat(ex.failure()).isEqualTo(GeminiClient.Failure.BUSY));
        server.verify();
    }

    @Test void quick503CanRecoverWithoutExposingProviderBody() throws Exception {
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("private upstream detail"));
        server.expect(anything()).andRespond(withSuccess(response("ANSWER",List.of("SP02"),"STOP"),MediaType.APPLICATION_JSON));
        assertThat(client.answer("Câu hỏi",context).status()).isEqualTo("ANSWER");
        server.verify();
    }

    @Test void researchUsesSearchToolWithoutHistoryOrJsonSchema() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(client, "webSearchEnabled", true);
        server.expect(anything()).andExpect(jsonPath("$.tools[0].google_search").exists())
            .andExpect(jsonPath("$.generationConfig.responseMimeType").doesNotExist())
            .andExpect(jsonPath("$.contents[0].parts[0].text").value("{\"question\":\"Cà phê rang sáng là gì?\"}"))
            .andRespond(withSuccess(mapper.writeValueAsString(new GroundedAnswerTest().response("https://example.org/coffee", true, "STOP")), MediaType.APPLICATION_JSON));
        assertThat(client.research("Cà phê rang sáng là gì?").status()).isEqualTo("ANSWER");
        server.verify();
    }
    @Test void disabledSearchMakesNoNetworkRequest() {
        assertThat(client.research("Cà phê rang sáng là gì?").status()).isEqualTo("INSUFFICIENT");
        server.verify();
    }

}
