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
    @Test void publicPageAndSourceRenderWithoutModel() throws Exception {
        mvc.perform(get("/chat")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("chat-input")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf"))).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/chat/sources/SRC07")).andExpect(status().isOk());
        mvc.perform(get("/chat/sources/nonexistent")).andExpect(status().isNotFound());
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
}
