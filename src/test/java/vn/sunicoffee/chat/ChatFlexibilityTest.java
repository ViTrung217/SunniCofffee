package vn.sunicoffee.chat;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatFlexibilityTest {
    @Test void correctionsAndComparisonsDoNotBecomePositivePreferences() {
        var history = List.of(new ChatConversation.Turn("pha máy, 2 người, 2 triệu, arabica", ""));
        assertThat(AdviceNeeds.from(List.of(), "Tôi không muốn pha máy, 2 người, 2 triệu").brewing()).isEmpty();
        assertThat(AdviceNeeds.from(history, "không pha máy nữa").brewing()).isEmpty();
        assertThat(AdviceNeeds.from(history, "không pha máy mà pha phin").brewing()).isEqualTo("pha phin");
        var comparison = AdviceNeeds.from(history, "Arabica khác Robusta thế nào?");
        assertThat(comparison.beans()).isEqualTo("arabica");
        assertThat(AdviceNeeds.preferenceStatement("Arabica khác Robusta thế nào?")).isFalse();
        assertThat(AdviceNeeds.preferenceStatement("tôi bị dị ứng arabica")).isFalse();
        assertThat(AdviceNeeds.from(List.of(), "Tôi muốn mua máy pha, 2 người, 2 triệu").brewing()).isEqualTo("pha may");
    }
    @Test void currencyFormatsSurviveRedactionButContactsDoNot() {
        for (String amount : List.of("2.000.000 đồng", "2,000,000 VND", "2 000 000 đ", "2000000 đồng", "2 triệu")) {
            String q = "pha máy, 2 người, " + amount;
            assertThat(AdviceNeeds.from(List.of(), q).budgetVnd()).isEqualByComparingTo("2000000");
            assertThat(ChatPrivacy.redact(q)).isEqualTo(q);
        }
        String mixed = ChatPrivacy.redact("Ngân sách 2.000.000 đồng, email me@example.test, gọi 0900123456");
        assertThat(mixed).contains("2.000.000 đồng", "[email đã ẩn]", "[số liên hệ đã ẩn]").doesNotContain("me@example.test", "0900123456");
    }
    @Test void topicChangeBypassesCannedMachineReplyAndResearchGetsOnlyPublicQuery() throws Exception {
        var gemini = mock(GeminiClient.class); var data = mock(ChatDataService.class);
        var service = new ChatService(new KnowledgeBase(new ObjectMapper()), gemini, data);
        when(gemini.ready()).thenReturn(true);
        String question = "Arabica khác Robusta thế nào?", query = "So sánh hạt cà phê Arabica và Robusta";
        when(gemini.answer(eq(question), anyList(), anyList())).thenReturn(new GeminiClient.Answer("RESEARCH", "Tra cứu", List.of(), query));
        when(gemini.research(query)).thenReturn(new ChatReply("ANSWER", "So sánh có nguồn", List.of(new ChatReply.Citation("W1", "Nguồn", "https://example.org/coffee"))));
        var conversation = new ChatConversation(); conversation.remember("pha máy, 2 người, 2 triệu", "máy mẫu");
        assertThat(service.answer(question, null, conversation).answer()).isEqualTo("So sánh có nguồn");
        verifyNoInteractions(data); verify(gemini).research(query);
        assertThat(conversation.history()).hasSize(2);
    }
    @Test void privateOrderAndStorePolicyNeverReachSearch() throws Exception {
        var gemini = mock(GeminiClient.class); var data = mock(ChatDataService.class);
        var service = new ChatService(new KnowledgeBase(new ObjectMapper()), gemini, data);
        assertThat(service.answer("Tra Google đơn SUNI-123", null).status()).isEqualTo("AUTH_REQUIRED");
        assertThat(service.answer("Tìm web chính sách đổi trả SuniCoffee", null).status()).isEqualTo("INSUFFICIENT");
        verifyNoInteractions(gemini, data);
    }
    @Test void modelCannotSendStoreOrPrivateResearchQuery() throws Exception {
        var gemini = mock(GeminiClient.class); var data = mock(ChatDataService.class);
        var service = new ChatService(new KnowledgeBase(new ObjectMapper()), gemini, data);
        when(gemini.ready()).thenReturn(true);
        for (String query : List.of("cà phê giao đến user@example.test", "tồn kho cửa hàng SuniCoffee", "cà phê gọi 0900123456")) {
            when(gemini.answer(anyString(), anyList())).thenReturn(new GeminiClient.Answer("RESEARCH", "Tra cứu", List.of(), query));
            assertThat(service.answer("Cà phê rang sáng là gì?", null).status()).isEqualTo("INSUFFICIENT");
        }
        verify(gemini, never()).research(anyString());
    }
    @Test void failedResearchDoesNotPolluteConversation() throws Exception {
        var gemini = mock(GeminiClient.class); var data = mock(ChatDataService.class);
        var service = new ChatService(new KnowledgeBase(new ObjectMapper()), gemini, data);
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenReturn(new GeminiClient.Answer("RESEARCH", "Tra cứu", List.of(), "Cà phê rang sáng là gì?"));
        when(gemini.research(anyString())).thenThrow(new GeminiClient.Unavailable(GeminiClient.Failure.QUOTA_EXCEEDED));
        var conversation = new ChatConversation();
        var failed = service.answer("Cà phê rang sáng là gì?", null, conversation);
        assertThat(failed.status()).isEqualTo("UNAVAILABLE");
        assertThat(failed.answer()).contains("Google đang giới hạn lượt tra cứu web", "chưa kiểm chứng");
        assertThat(conversation.history()).isEmpty();
    }
    @Test void unrelatedKnowledgeReplyDoesNotReceiveCatalogueDisclaimer() throws Exception {
        var gemini = mock(GeminiClient.class); var data = mock(ChatDataService.class);
        var service = new ChatService(new KnowledgeBase(new ObjectMapper()), gemini, data);
        when(gemini.ready()).thenReturn(true);
        when(data.adviceContext(any())).thenReturn(new ChatDataService.AdviceContext(List.of(),
            Map.of("STORE1", new ChatReply.Citation("STORE1", "Máy mẫu", "/products/1")), true, null));
        when(gemini.answer(anyString(), anyList(), anyList())).thenReturn(new GeminiClient.Answer("ANSWER", "Picopresso không tự đun nước.", List.of("SP01")));
        var conversation = new ChatConversation(); conversation.remember("pha máy, 2 người, 2 triệu", "máy mẫu");
        assertThat(service.answer("Picopresso có tự đun nước không?", null, conversation).answer())
            .isEqualTo("Picopresso không tự đun nước.");
    }

}
