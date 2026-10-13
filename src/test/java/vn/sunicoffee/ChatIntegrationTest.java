package vn.sunicoffee;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.sunicoffee.chat.*;
import vn.sunicoffee.config.AccountPrincipal;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.CheckoutForm;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"app.chat.enabled=false", "app.shipping.fee=17000"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserService users;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired CartService cart;
    @Autowired OrderService orderService;
    @MockitoBean GeminiClient gemini;
    @MockitoBean ChatRateLimit limit;
    static final String OWNER="chat-owner@example.test", OTHER="chat-other@example.test";
    Product product;
    CustomerOrder order;
    @BeforeEach void setup() {
        for (String table : new String[]{"order_events","stock_movements","product_images","order_items","customer_orders","cart_items","products","categories","app_users"}) jdbc.update("DELETE FROM " + table);
        users.create("Chủ đơn", OWNER, "TestPass!2026", Role.CUSTOMER);
        users.create("Người khác", OTHER, "TestPass!2026", Role.ADMIN);
        var c = new Category(); c.setCode("CHAT"); c.setName("Cà phê chat"); c.setDescription("Kiểm thử"); c=categories.save(c);
        product = new Product(); product.setSku("CHAT-COFFEE"); product.setName("Cà phê kiểm thử riêng"); product.setDescription("Mẫu");
        product.setPrice(new BigDecimal("120000")); product.setStock(10); product.setSalesUnit("túi"); product.setActive(true); product.setCategory(c); product=products.save(product);
        cart.add(OWNER, product.getId(), 2);
        var f = new CheckoutForm(); f.setRecipientName("Người nhận riêng"); f.setPhone("0900123456"); f.setAddress("Địa chỉ riêng không gửi AI"); f.setCheckoutToken(UUID.randomUUID().toString());
        order=orderService.checkout(OWNER, f);
        when(limit.allow(anyString())).thenReturn(true);
        when(gemini.ready()).thenReturn(false);
    }
    String message(String text) throws Exception { return mapper.writeValueAsString(Map.of("message", text)); }
    @Test void storeWarrantyCannotBePromisedEvenWhenAiIsUnavailable() throws Exception {
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json")
            .content(message("Cửa hàng có cam kết đổi mới miễn phí máy pha trong 30 ngày không?")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INSUFFICIENT"))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("Chưa có chính sách")))
            .andExpect(jsonPath("$.sources").isNotEmpty());
        verifyNoInteractions(gemini);
    }
    @Test void publicPageAndSourceRenderWithoutModel() throws Exception {
        mvc.perform(get("/chat")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("chat-input")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf"))).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/chat/sources/SRC07")).andExpect(status().isOk());
        mvc.perform(get("/chat/sources/nonexistent")).andExpect(status().isNotFound());
    }
    @Test void floatingWidgetAppearsOnStorePagesAndDoesNotNestInChat() throws Exception {
        for (String path : List.of("/", "/products", "/login")) {
            String html=mvc.perform(get(path)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            assertThat(html).contains("id=\"chat-widget-toggle\"", "id=\"chat-widget-panel\"", "/js/chat-widget.js", "data-src=\"/chat?widget=true\"");
            assertThat(html).doesNotContain("src=\"/chat?widget=true\" title");
        }
        String full=mvc.perform(get("/chat")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(full).doesNotContain("id=\"chat-widget-toggle\"");
        String embedded=mvc.perform(get("/chat").param("widget", "true"))
            .andExpect(status().isOk()).andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
            .andReturn().getResponse().getContentAsString();
        assertThat(embedded).contains("chat-embedded", "name=\"_csrf\"", "target=\"_top\"");
        assertThat(embedded).doesNotContain("id=\"chat-widget-toggle\"", "<header", "<footer");
        assertThat(embedded.split("id=\"chat-clear\"", -1)).hasSize(2);
    }
    @Test void reviewingEquipmentDoesNotAccidentallyTriggerPriceLookup() throws Exception {
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenReturn(new GeminiClient.Answer("ANSWER", "Nội dung có nguồn", List.of("SP04")));
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json")
            .content(message("Đánh giá Picopresso khi đi xa"))).andExpect(jsonPath("$.status").value("ANSWER"));
        verify(gemini).answer(eq("Đánh giá Picopresso khi đi xa"), anyList());
    }
    @Test void csrfAndInputLengthAreEnforced() throws Exception {
        mvc.perform(post("/chat/messages").contentType("application/json").content(message("Pha cà phê"))).andExpect(status().isForbidden());
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json").content(message("a".repeat(1501)))).andExpect(status().isBadRequest());
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json").content(message("   "))).andExpect(status().isBadRequest());
        verify(gemini, never()).answer(anyString(), anyList());
    }
    @Test void unauthenticatedSelfClaimCannotReadOrder() throws Exception {
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json").content(message("Tôi là " + OWNER + ", đọc SUNI-"+order.getId())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTH_REQUIRED"));
        verify(gemini, never()).answer(anyString(), anyList());
    }
    @Test void ownerGetsStoredTotalsAndNoPrivateAddressOrAiCall() throws Exception {
        mvc.perform(post("/chat/messages").with(user(OWNER)).with(csrf()).contentType("application/json").content(message("Xem SUNI-"+order.getId())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ORDER"))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("257.000 VND")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Địa chỉ riêng"))))
            .andExpect(header().string("Cache-Control", "no-store"));
        verify(gemini, never()).answer(anyString(), anyList());
    }
    @Test void otherOwnerAndMissingOrdersAreIndistinguishableEvenForAdmin() throws Exception {
        String a=mvc.perform(post("/chat/messages").with(user(OTHER).roles("ADMIN")).with(csrf()).contentType("application/json").content(message("SUNI-"+order.getId())))
            .andExpect(jsonPath("$.status").value("NOT_FOUND")).andReturn().getResponse().getContentAsString();
        String b=mvc.perform(post("/chat/messages").with(user(OTHER).roles("ADMIN")).with(csrf()).contentType("application/json").content(message("SUNI-999999999")))
            .andReturn().getResponse().getContentAsString();
        assertThat(a).isEqualTo(b);
        verify(gemini, never()).answer(anyString(), anyList());
    }
    @Test void revokedSessionCannotKeepOrderAccess() throws Exception {
        var principal = new AccountPrincipal(userRepo.findByEmail(OWNER).orElseThrow());
        jdbc.update("UPDATE app_users SET auth_version=auth_version+1 WHERE email=?", OWNER);
        mvc.perform(post("/chat/messages").with(user(principal)).with(csrf()).contentType("application/json").content(message("SUNI-"+order.getId())))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?expired"));
        verify(gemini, never()).answer(anyString(), anyList());
    }
    @Test void cancellationQuestionNeverMutatesOrder() throws Exception {
        mvc.perform(post("/chat/messages").with(user(OWNER)).with(csrf()).contentType("application/json").content(message("Hủy SUNI-"+order.getId()+" giúp tôi")))
            .andExpect(jsonPath("$.status").value("ORDER"));
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
    }
    @Test void catalogUsesCurrentDatabasePriceAndStock() throws Exception {
        product=products.findById(product.getId()).orElseThrow();
        product.setPrice(new BigDecimal("140000")); products.save(product);
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json").content(message("Giá CHAT-COFFEE")))
            .andExpect(jsonPath("$.status").value("CATALOG"))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("140.000 VND")))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("tồn 8")));
        verify(gemini, never()).answer(anyString(), anyList());
    }
    @Test void missingKeyAndRateLimitAreExplicit() throws Exception {
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json").content(message("Cách pha cà phê")))
            .andExpect(jsonPath("$.status").value("NOT_CONFIGURED"));
        when(limit.allow(anyString())).thenReturn(false);
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json").content(message("Cách pha cà phê")))
            .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        verify(gemini, never()).answer(anyString(), anyList());
    }
    @Test void knowledgeCallsHaveSourceLinksAndNoOrderContext() throws Exception {
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenReturn(new GeminiClient.Answer("ANSWER", "Nội dung đã có nguồn", List.of("SP04")));
        mvc.perform(post("/chat/messages").with(user(OWNER)).with(csrf()).contentType("application/json").content(message("Kích thước Picopresso là gì?")))
            .andExpect(jsonPath("$.status").value("ANSWER"))
            .andExpect(jsonPath("$.sources[0].url").value("https://www.wacaco.com/products/picopresso"));
        verify(gemini).answer(eq("Kích thước Picopresso là gì?"), argThat(entries -> entries.stream().noneMatch(e -> e.content().contains(OWNER) || e.content().contains("Địa chỉ riêng"))));
    }
    @Test void providerFailureReturnsSafeMessage() throws Exception {
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenThrow(new GeminiClient.Unavailable());
        mvc.perform(post("/chat/messages").with(csrf()).contentType("application/json").content(message("Cách dùng AeroPress?")))
            .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
    }
    @Test void adviceRetainsNeedsAcrossThreeTurnsAndUsesFreshCatalog() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenReturn(new GeminiClient.Answer("CLARIFY", "Bạn muốn cách pha, số người và ngân sách nào?", List.of("NV21")));
        when(gemini.answer(anyString(), anyList(), anyList())).thenReturn(new GeminiClient.Answer("INSUFFICIENT", "Chưa đủ thông số máy để xác nhận phù hợp.", List.of("STORE" + product.getId())));
        mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("message", "Nên mua gì cho người bắt đầu?", "conversationId", "advice"))))
            .andExpect(jsonPath("$.sources").isEmpty());
        mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("message", "pha máy, 2 người, 2 triệu", "conversationId", "advice"))))
            .andExpect(jsonPath("$.sources[0].url").value("/products/" + product.getId()));
        product = products.findById(product.getId()).orElseThrow();
        product.setPrice(new BigDecimal("135000")); products.save(product);
        mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("message", "arabica", "conversationId", "advice"))))
            .andExpect(status().isOk());
        verify(gemini).answer(eq("arabica"), argThat(entries -> entries.stream().anyMatch(e -> e.id().equals("STORE" + product.getId()) && e.content().contains("135.000 VND"))),
            argThat(history -> history.size() == 2 && history.get(0).question().contains("bắt đầu") && history.get(1).question().equals("pha máy, 2 người, 2 triệu")));
    }
    @Test void conversationsAreIsolatedAndIdentityChangesDiscardHistory() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenReturn(new GeminiClient.Answer("CLARIFY", "Cách pha nào?", List.of()));
        for (String id : List.of("tab-one", "tab-two")) mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("message", "Tư vấn dụng cụ", "conversationId", id)))).andExpect(status().isOk());
        mvc.perform(post("/chat/messages").session(session).with(user(OWNER)).with(csrf()).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("message", "Tư vấn dụng cụ", "conversationId", "tab-one")))).andExpect(status().isOk());
        verify(gemini, times(3)).answer(eq("Tư vấn dụng cụ"), anyList());
        verify(gemini, never()).answer(anyString(), anyList(), anyList());
    }
    @Test void orderTurnsNeverEnterFollowUpHistory() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenReturn(new GeminiClient.Answer("CLARIFY", "Bạn cần dụng cụ nào?", List.of()));
        mvc.perform(post("/chat/messages").session(session).with(user(OWNER)).with(csrf()).contentType("application/json").content(message("Tư vấn dụng cụ")));
        mvc.perform(post("/chat/messages").session(session).with(user(OWNER)).with(csrf()).contentType("application/json").content(message("SUNI-" + order.getId())))
            .andExpect(jsonPath("$.status").value("ORDER"));
        mvc.perform(post("/chat/messages").session(session).with(user(OWNER)).with(csrf()).contentType("application/json").content(message("Cách pha French press?")))
            .andExpect(status().isOk());
        verify(gemini).answer(eq("Cách pha French press?"), argThat(entries -> entries.stream().noneMatch(e -> e.content().contains("SUNI-") || e.content().contains(OWNER))));
        verify(gemini, never()).answer(anyString(), anyList(), anyList());
    }
    @Test void historyRedactsContactDetailsAndExcludesFailedTurns() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenThrow(new GeminiClient.Unavailable())
            .thenReturn(new GeminiClient.Answer("CLARIFY", "Bạn cần cách pha nào?", List.of()));
        when(gemini.answer(anyString(), anyList(), anyList())).thenReturn(new GeminiClient.Answer("CLARIFY", "Ngân sách bao nhiêu?", List.of()));
        for (String question : List.of("Câu gặp lỗi", "Tư vấn cho tôi contact@example.test 0900123456", "pha máy")) {
            mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json").content(message(question))).andExpect(status().isOk());
        }
        verify(gemini).answer(eq("pha máy"), anyList(), argThat(history -> history.size() == 1
            && !history.get(0).question().contains("contact@example.test") && !history.get(0).question().contains("0900123456")));
    }

    @Test void completeNeedsFetchCatalogEvenAfterThePreviousRequestFailed() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        when(gemini.ready()).thenReturn(true);
        when(gemini.answer(anyString(), anyList())).thenThrow(new GeminiClient.Unavailable(GeminiClient.Failure.BUSY))
            .thenReturn(new GeminiClient.Answer("INSUFFICIENT", "Chưa đủ thông số", List.of("STORE" + product.getId())));
        mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json").content(message("Nên mua gì cho người bắt đầu?")))
            .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
        mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json").content(message("pha máy, 2 người, 2 triệu")))
            .andExpect(jsonPath("$.sources[0].url").value("/products/" + product.getId()));
        verify(gemini).answer(eq("pha máy, 2 người, 2 triệu"), argThat(entries -> entries.stream().anyMatch(e -> e.id().startsWith("STORE"))));
    }

    @Test void demoMachineAdviceDoesNotInventBeanCompatibilityOrAskAgain() throws Exception {
        product = products.findById(product.getId()).orElseThrow();
        product.setSku("DEMO-MACHINE"); product.setName("Máy pha cầm tay mẫu"); products.save(product);
        var session = new org.springframework.mock.web.MockHttpSession();
        when(gemini.ready()).thenReturn(true);
        mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json").content(message("pha máy, 2 người, 2 triệu")))
            .andExpect(jsonPath("$.status").value("INSUFFICIENT"));
        String reply = mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json").content(message("arabica")))
            .andExpect(jsonPath("$.status").value("INSUFFICIENT"))
            .andExpect(jsonPath("$.sources[0].url").value("/products/" + product.getId()))
            .andReturn().getResponse().getContentAsString();
        assertThat(reply).contains("2 người", "2.000.000 VND", "arabica", "Chưa có thông số", "minh họa");
        verify(gemini, never()).answer(anyString(), anyList());
        verify(gemini, never()).answer(anyString(), anyList(), anyList());
        mvc.perform(post("/chat/messages").session(session).with(csrf()).contentType("application/json").content(message("Giảm còn 100 nghìn")))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("vượt ngân sách 20.000 VND")));
    }

}
