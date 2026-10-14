package vn.sunicoffee;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.UserRepository;
import vn.sunicoffee.service.UserService;
import vn.sunicoffee.support.SupportService;
import vn.sunicoffee.config.AccountPrincipal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SupportIntegrationTest {
    @Autowired MockMvc mvc; @Autowired SupportService service; @Autowired UserService users;
    @Autowired UserRepository accounts; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper mapper;
    static final String CUSTOMER="support-customer@example.test", OTHER="support-other@example.test", ADMIN="support-admin@example.test";
    @BeforeEach void setup() {
        users.create("Khách hỗ trợ",CUSTOMER,"TestPass!2026",Role.CUSTOMER);
        users.create("Khách khác",OTHER,"TestPass!2026",Role.CUSTOMER);
        users.create("Nhân viên",ADMIN,"TestPass!2026",Role.ADMIN);
    }
    String body(String content,String token) throws Exception {return mapper.writeValueAsString(Map.of("content",content,"requestToken",token));}
    String body(String content) throws Exception {return body(content,UUID.randomUUID().toString());}
    long thread() {return service.messages(CUSTOMER,null,false,0,0).conversation().id();}
    @Test void customerAndAdminExchangePersistedMessagesWithExplicitReadAcknowledgements() throws Exception {
        mvc.perform(post("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body("Tôi cần hỏi về đơn hàng")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fromAdmin").value(false));
        long id=thread();
        mvc.perform(get("/admin/support/"+id+"/messages").with(user(ADMIN).roles("ADMIN")))
            .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.conversation.unread").value(1));
        assertThat(service.inbox(ADMIN,"","unread",0).conversations()).hasSize(1); // GET doesn't mark read.
        var first=service.messages(ADMIN,id,true,0,0).messages().getFirst();
        mvc.perform(post("/admin/support/"+id+"/read").with(user(ADMIN).roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"throughId\":"+first.id()+"}"))
            .andExpect(status().isOk());
        assertThat(service.inbox(ADMIN,"","unread",0).conversations()).isEmpty();
        mvc.perform(post("/admin/support/"+id+"/messages").with(user(ADMIN).roles("ADMIN")).with(csrf()).contentType("application/json").content(body("Cửa hàng đã nhận được yêu cầu.")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fromAdmin").value(true));
        mvc.perform(get("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")))
            .andExpect(jsonPath("$.messages.length()").value(2)).andExpect(jsonPath("$.conversation.unread").value(1));
    }
    @Test void otherCustomerCannotReadOrReplyToAnotherConversationOrForgeSender() throws Exception {
        var first=service.send(CUSTOMER,null,false,"Nội dung riêng",UUID.randomUUID().toString());long id=thread();
        mvc.perform(get("/support/messages").param("conversationId",Long.toString(id)).with(user(OTHER).roles("CUSTOMER")))
            .andExpect(jsonPath("$.messages").isEmpty());
        mvc.perform(get("/admin/support/"+id+"/messages").with(user(OTHER).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/support/"+id+"/messages").with(user(OTHER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body("Giả mạo"))).andExpect(status().isForbidden());
        String forged=mapper.writeValueAsString(Map.of("content","Tin của tôi","requestToken",UUID.randomUUID().toString(),"fromAdmin",true,"conversationId",id,"senderId",accounts.findByEmail(ADMIN).orElseThrow().getId()));
        mvc.perform(post("/support/messages").with(user(OTHER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content(forged))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fromAdmin").value(false)).andExpect(jsonPath("$.senderName").value("Khách khác"));
        assertThat(service.messages(CUSTOMER,null,false,0,0).messages()).containsExactly(first);
    }
    @Test void csrfLoginLengthAndCursorsAreValidated() throws Exception {
        mvc.perform(get("/support")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/support").with(user(CUSTOMER).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")).contentType("application/json").content(body("Tin thiếu CSRF"))).andExpect(status().isForbidden());
        for(String text:List.of(" ","x".repeat(1501))) mvc.perform(post("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body(text))).andExpect(status().isBadRequest());
        mvc.perform(get("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")).param("after","1").param("before","2")).andExpect(status().isBadRequest());
        assertThat(service.messages(CUSTOMER,null,false,0,0).conversation()).isNull();
    }
    @Test void retryIsIdempotentAndDifferentContentWithSameTokenConflicts() throws Exception {
        String token=UUID.randomUUID().toString();
        for(int i=0;i<2;i++) mvc.perform(post("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body("Tin gửi lại",token))).andExpect(status().isOk());
        assertThat(service.messages(CUSTOMER,null,false,0,0).messages()).hasSize(1);
        assertThat(service.inbox(ADMIN,"","all",0).conversations().getFirst().unread()).isEqualTo(1);
        mvc.perform(post("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body("Nội dung khác",token))).andExpect(status().isConflict());
    }
    @Test void firstSendRetryAndReloadReturnTheSamePersistedTimestamp() {
        // Linux clocks can produce nanoseconds; H2/MySQL TIMESTAMP precision is lower.
        var instant=LocalDateTime.of(2026,10,14,14,30,0,123456789);
        String token=UUID.randomUUID().toString();
        try(var clock=org.mockito.Mockito.mockStatic(LocalDateTime.class,org.mockito.Mockito.CALLS_REAL_METHODS)) {
            clock.when(()->LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))).thenReturn(instant);
            var sent=service.send(CUSTOMER,null,false,"Kiểm tra thời điểm",token);
            var retry=service.send(CUSTOMER,null,false,"Kiểm tra thời điểm",token);
            var loaded=service.messages(CUSTOMER,null,false,0,0).messages().getFirst();
            assertThat(sent).isEqualTo(loaded);
            assertThat(retry).isEqualTo(loaded);
        }
    }
    @Test void closedConversationCanBeReopenedByCustomerWithoutLosingHistory() throws Exception {
        service.send(CUSTOMER,null,false,"Câu đầu",UUID.randomUUID().toString());long id=thread();
        mvc.perform(post("/admin/support/"+id+"/state").with(user(ADMIN).roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"closed\":true}")).andExpect(status().isOk());
        mvc.perform(post("/admin/support/"+id+"/messages").with(user(ADMIN).roles("ADMIN")).with(csrf()).contentType("application/json").content(body("Tin sau đóng"))).andExpect(status().isConflict());
        service.send(CUSTOMER,null,false,"Cần hỗ trợ thêm",UUID.randomUUID().toString());
        var result=service.messages(CUSTOMER,null,false,0,0);
        assertThat(result.conversation().closed()).isFalse();assertThat(result.conversation().id()).isEqualTo(id);assertThat(result.messages()).hasSize(2);
    }
    @Test void readCursorDoesNotConsumeNewMessagesOrMoveBackwards() throws Exception {
        var first=service.send(CUSTOMER,null,false,"Đầu",UUID.randomUUID().toString());long id=thread();
        var second=service.send(CUSTOMER,null,false,"Mới",UUID.randomUUID().toString());
        service.read(ADMIN,id,true,first.id()); assertThat(service.inbox(ADMIN,"","all",0).conversations().getFirst().unread()).isEqualTo(1);
        service.read(ADMIN,id,true,second.id());service.read(ADMIN,id,true,first.id());
        assertThat(service.inbox(ADMIN,"","unread",0).conversations()).isEmpty();
        mvc.perform(post("/support/read").with(user(CUSTOMER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content("{\"throughId\":999999999}"))
            .andExpect(status().isBadRequest());
    }
    @Test void historyIsPagedAndPollingCatchesUpWithoutSkippingMessages() {
        service.send(CUSTOMER,null,false,"First",UUID.randomUUID().toString());long id=thread();long customerId=accounts.findByEmail(CUSTOMER).orElseThrow().getId();
        for(int i=1;i<61;i++) jdbc.update("INSERT INTO support_messages(conversation_id,sender_id,sender_name,from_admin,content,request_token,created_at) VALUES (?,?,?,FALSE,?,?,?)",id,customerId,"Khách","Tin "+i,UUID.randomUUID().toString(),LocalDateTime.now().minusMinutes(2));
        var latest=service.messages(CUSTOMER,null,false,0,0);assertThat(latest.messages()).hasSize(50);assertThat(latest.hasOlder()).isTrue();
        var older=service.messages(CUSTOMER,null,false,0,latest.messages().getFirst().id());assertThat(older.messages()).hasSize(11);assertThat(older.hasOlder()).isFalse();
        var catchup=service.messages(CUSTOMER,null,false,older.messages().getFirst().id(),0);assertThat(catchup.messages()).hasSize(50);assertThat(catchup.hasMore()).isTrue();
        var remaining=service.messages(CUSTOMER,null,false,catchup.messages().getLast().id(),0);assertThat(remaining.messages()).hasSize(10);assertThat(remaining.hasMore()).isFalse();
    }
    @Test void rateLimitAllowsRetryButRejectsMoreNewMessages() throws Exception {
        String token=UUID.randomUUID().toString();service.send(CUSTOMER,null,false,"First",token);
        for(int i=1;i<20;i++) service.send(CUSTOMER,null,false,"Tin "+i,UUID.randomUUID().toString());
        assertThat(service.send(CUSTOMER,null,false,"First",token)).isNotNull();
        mvc.perform(post("/support/messages").with(user(CUSTOMER).roles("CUSTOMER")).with(csrf()).contentType("application/json").content(body("Too many"))).andExpect(status().isTooManyRequests());
    }
    @Test void disabledAccountCannotContinueUsingConversation() throws Exception {
        service.send(CUSTOMER,null,false,"Trước khóa",UUID.randomUUID().toString());
        var account=accounts.findByEmail(CUSTOMER).orElseThrow();var principal=new AccountPrincipal(account);
        account.setEnabled(false);accounts.saveAndFlush(account);
        mvc.perform(get("/support/messages").with(user(principal))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?expired"));
    }
    @Test void pagesRenderSeparateChannelsAndInboxEscapesContent() throws Exception {
        service.send(CUSTOMER,null,false,"<img src=x onerror=alert(1)>",UUID.randomUUID().toString());long id=thread();
        mvc.perform(get("/admin/support").with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;img")));
        mvc.perform(get("/admin/support/"+id).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("support-toggle-state")));
        String embedded=mvc.perform(get("/support").param("widget","true").with(user(CUSTOMER).roles("CUSTOMER"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(embedded).contains("support-embedded", "support-input").doesNotContain("<header", "<footer", "Quản trị", "chat-widget-toggle");
        mvc.perform(get("/").with(user(CUSTOMER).roles("CUSTOMER"))).andExpect(content().string(org.hamcrest.Matchers.containsString("support-widget-frame")));
    }
    @Test void inboxFiltersAndUnknownThreadAreHandled() throws Exception {
        service.send(CUSTOMER,null,false,"Xin chào",UUID.randomUUID().toString());long id=thread();
        assertThat(service.inbox(ADMIN,"support-customer","open",0).conversations()).hasSize(1);
        assertThat(service.inbox(ADMIN,"nobody","all",0).conversations()).isEmpty();
        service.close(ADMIN,id,true);assertThat(service.inbox(ADMIN,"","closed",0).conversations()).hasSize(1);
        mvc.perform(get("/admin/support/999999999/messages").with(user(ADMIN).roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get("/admin/support/threads").param("state","oops").with(user(ADMIN).roles("ADMIN"))).andExpect(status().isBadRequest());
    }
}
