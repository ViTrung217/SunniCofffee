package vn.sunicoffee;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.CheckoutForm;
import vn.sunicoffee.payment.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.sepay.enabled=true","app.sepay.sandbox=true","app.sepay.api-token=test-only-not-real",
    "app.sepay.bank-account-id=11111111-1111-1111-1111-111111111111","app.sepay.account-number=0123456789",
    "app.sepay.qr-account=0123456789","app.sepay.bank=Vietcombank","app.sepay.holder=TEST ACCOUNT","app.shipping.fee=25000"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class SepayPaymentIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired UserService users;
    @Autowired ProductRepository products; @Autowired CategoryRepository categories; @Autowired OrderRepository orders;
    @Autowired OrderService orderService; @Autowired CartService carts; @Autowired PaymentStore store;
    @Autowired PaymentService payments; @Autowired SepaySettings settings;
    @MockitoBean SepayClient client;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    String customer,other,admin; Product product;
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString();customer="payer-"+suffix+"@example.test";other="other-"+suffix+"@example.test";admin="admin-"+suffix+"@example.test";
        users.create("Người mua",customer,"TestPass!2026",Role.CUSTOMER);users.create("Người khác",other,"TestPass!2026",Role.CUSTOMER);users.create("Quản trị",admin,"TestPass!2026",Role.ADMIN);
        var c=new Category();c.setCode("PAY-"+suffix.substring(0,8));c.setName("Kiểm thử QR");c.setDescription("Mẫu");c=categories.save(c);
        product=new Product();product.setSku("PAY-"+suffix.substring(0,8));product.setName("Cà phê");product.setDescription("Mẫu");product.setCategory(c);product.setPrice(new BigDecimal("95000"));product.setStock(10);product.setSalesUnit("túi");product.setActive(true);product=products.save(product);
    }
    CheckoutForm form(PaymentMethod method) {var f=new CheckoutForm();f.setCheckoutToken(UUID.randomUUID().toString());f.setRecipientName("Người mua");f.setPhone("0900000000");f.setAddress("Địa chỉ nhận hàng mẫu");f.setPaymentMethod(method);return f;}
    CustomerOrder buy(PaymentMethod method) {carts.add(customer,product.getId(),1);var o=orderService.checkout(customer,form(method));em.flush();return o;}
    CustomerOrder buy() {return buy(PaymentMethod.SEPAY);}
    SepayClient.Transfer transfer(CustomerOrder o,String amount) {return new SepayClient.Transfer(UUID.randomUUID().toString(),o.getSepayBankAccountId(),o.getSepayAccountNumber(),"","in",new BigDecimal(amount),BigDecimal.ZERO,"CK "+o.getPaymentCode(),LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")),"TEST-REF");}
    @Test void checkoutRedirectsDirectlyToQrAndReplayKeepsSameOrder() throws Exception {
        carts.add(customer,product.getId(),1);
        var page=mvc.perform(get("/checkout").with(user(customer))).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)page.getRequest().getSession();
        var form=(CheckoutForm)page.getModelAndView().getModel().get("form");
        String destination=null;
        for(int i=0;i<2;i++) {
            var result=mvc.perform(post("/checkout").session(session).with(user(customer)).with(csrf())
                .param("checkoutToken",form.getCheckoutToken()).param("recipientName","Người mua")
                .param("phone","0900000000").param("address","Địa chỉ nhận hàng mẫu").param("paymentMethod","SEPAY"))
                .andExpect(status().is3xxRedirection()).andReturn();
            String url=result.getResponse().getRedirectedUrl();
            assertThat(url).matches("/orders/[0-9]+/payment");
            if(destination!=null) assertThat(url).isEqualTo(destination);
            destination=url;
        }
    }
    @Test void successPageRequiresOwnerAndConfirmedExactPayment() throws Exception {
        var o=buy();String base="/orders/"+o.getId()+"/payment";
        mvc.perform(get(base+"/success").with(user(other))).andExpect(status().isNotFound());
        mvc.perform(get(base+"/success").with(user(customer))).andExpect(redirectedUrl(base));
        store.apply(o.getId(),List.of(transfer(o,"10000")));
        mvc.perform(get(base+"/success").with(user(customer))).andExpect(redirectedUrl(base));
        store.apply(o.getId(),List.of(transfer(o,"110000")));
        mvc.perform(get(base).with(user(customer))).andExpect(redirectedUrl(base+"/success"));
        mvc.perform(get(base+"/success").with(user(customer))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tiếp tục mua sắm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Về trang chủ")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Thanh toán mô phỏng thành công")));
        orderService.transition(o.getId(),OrderStatus.CANCELLED,admin,null,null,"Khách yêu cầu hủy");
        mvc.perform(get(base+"/success").with(user(customer))).andExpect(redirectedUrl(base));
    }
    @Test void qrCheckoutSnapshotsTotalAndDestinationAndIsIdempotent() throws Exception {
        carts.add(customer,product.getId(),1);var f=form(PaymentMethod.SEPAY);var o=orderService.checkout(customer,f);em.flush();
        assertThat(orderService.checkout(customer,f).getId()).isEqualTo(o.getId());
        assertThat(o.getTotal()).isEqualByComparingTo("120000");assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(o.getPaymentCode()).matches("SC[A-F0-9]{20}");assertThat(o.getSepayBankAccountId()).isEqualTo(settings.bankAccountId());
        assertThat(product.getStock()).isEqualTo(9);
        mvc.perform(get("/orders/"+o.getId()+"/payment").with(user(customer))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sandbox — không chuyển tiền thật")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("https://vietqr.app/img"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-only-not-real"))));
        mvc.perform(get("/orders/"+o.getId()).with(user(customer))).andExpect(status().isOk());
        mvc.perform(get("/orders").with(user(customer))).andExpect(status().isOk());
        verifyNoInteractions(client);
    }
    @Test void exactPaymentAndRepeatedReceiptDoNotDoubleCredit() {
        var o=buy();var t=transfer(o,"120000");store.apply(o.getId(),List.of(t));store.apply(o.getId(),List.of(t));
        assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);assertThat(o.getReceivedAmount()).isEqualByComparingTo("120000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_receipts WHERE order_id=?",Integer.class,o.getId())).isEqualTo(1);
        assertThat(product.getStock()).isEqualTo(9);orderService.transition(o.getId(),OrderStatus.CONFIRMED,admin);
    }
    @Test void insufficientAndExcessFundsRemainUnderReview() {
        var o=buy();store.apply(o.getId(),List.of(transfer(o,"60000")));
        assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.REVIEW);
        assertThatThrownBy(()->orderService.transition(o.getId(),OrderStatus.CONFIRMED,admin)).isInstanceOf(BusinessException.class);
        store.apply(o.getId(),List.of(transfer(o,"60000")));assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        store.apply(o.getId(),List.of(transfer(o,"1")));assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.REVIEW);
    }
    @Test void unrelatedOrOutgoingTransfersCannotPayAnOrder() {
        var o=buy();var good=transfer(o,"120000");
        var bad=List.of(
            new SepayClient.Transfer(good.id(),UUID.randomUUID().toString(),good.accountNumber(),"","in",good.amountIn(),BigDecimal.ZERO,good.content(),good.at(),good.reference()),
            new SepayClient.Transfer(good.id(),good.bankAccountId(),"DIFFERENT","","in",good.amountIn(),BigDecimal.ZERO,good.content(),good.at(),good.reference()),
            new SepayClient.Transfer(good.id(),good.bankAccountId(),good.accountNumber(),"","out",good.amountIn(),good.amountIn(),good.content(),good.at(),good.reference()),
            new SepayClient.Transfer(good.id(),good.bankAccountId(),good.accountNumber(),"","in",good.amountIn(),BigDecimal.ZERO,good.content()+"0",good.at(),good.reference()),
            new SepayClient.Transfer(good.id(),good.bankAccountId(),good.accountNumber(),"","in",good.amountIn(),BigDecimal.ZERO,good.content()+" SC01234567890123456789",good.at(),good.reference()),
            new SepayClient.Transfer(good.id(),good.bankAccountId(),good.accountNumber(),"","in",good.amountIn(),BigDecimal.ZERO,good.content(),o.getCreatedAt().minusDays(1),good.reference()));
        store.apply(o.getId(),bad);assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);assertThat(store.view(o.getId(),customer,false).receipts()).isEmpty();
    }
    @Test void cancellationAndLatePaymentRequireRefundWithoutResurrectingOrder() {
        var o=buy();orderService.cancelByCustomer(o.getId(),customer);store.apply(o.getId(),List.of(transfer(o,"120000")));
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUIRED);assertThat(product.getStock()).isEqualTo(10);
        assertThat(store.view(o.getId(),customer,false).payable()).isFalse();
    }
    @Test void paidCustomerCannotSelfCancelButAdminCancellationFlagsRefund() {
        var o=buy();store.apply(o.getId(),List.of(transfer(o,"120000")));
        assertThatThrownBy(()->orderService.cancelByCustomer(o.getId(),customer)).isInstanceOf(BusinessException.class);
        orderService.transition(o.getId(),OrderStatus.CANCELLED,admin,"","","Khách yêu cầu hủy");
        assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUIRED);assertThat(product.getStock()).isEqualTo(10);
    }
    @Test void paymentEndpointsEnforceOwnershipRolesAndCsrf() throws Exception {
        var o=buy();String url="/orders/"+o.getId()+"/payment";
        mvc.perform(get(url)).andExpect(status().is3xxRedirection());mvc.perform(get(url).with(user(other))).andExpect(status().isNotFound());
        mvc.perform(post(url+"/check").with(user(other)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(url+"/check").with(user(customer))).andExpect(status().isForbidden());
        mvc.perform(post("/admin"+url+"/check").with(user(customer)).with(csrf())).andExpect(status().isForbidden());verifyNoInteractions(client);
    }
    @Test void checkUsesTrustedApiAndRateLimitsPerOrder() throws Exception {
        var o=buy();when(client.transfers(any())).thenReturn(List.of(transfer(o,"120000")));
        String url="/orders/"+o.getId()+"/payment/check";
        for(int i=0;i<2;i++) mvc.perform(post(url).with(user(customer)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        verify(client,times(1)).transfers(new SepayClient.Query(o.getPaymentCode(),o.getSepayBankAccountId(),true));
    }
    @Test void providerFailureDoesNotMarkPaidOrExposeDetails() throws Exception {
        var o=buy();when(client.transfers(any())).thenThrow(new SepayClient.Unavailable());
        mvc.perform(post("/orders/"+o.getId()+"/payment/check").with(user(customer)).with(csrf())).andExpect(status().isServiceUnavailable());
        assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }
    @Test void codRemainsIndependentAndOnlineOrdersCannotBeSettledAsCod() {
        var o=buy();assertThatThrownBy(()->orderService.settleCod(o.getId(),admin)).isInstanceOf(BusinessException.class);
        var cod=buy(PaymentMethod.COD);orderService.transition(cod.getId(),OrderStatus.CONFIRMED,admin);
        orderService.transition(cod.getId(),OrderStatus.SHIPPING,admin,"Test carrier","TEST123","");orderService.transition(cod.getId(),OrderStatus.COMPLETED,admin);orderService.settleCod(cod.getId(),admin);
        assertThat(cod.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);assertThat(cod.getReceivedAmount()).isEqualByComparingTo(cod.getTotal());verifyNoInteractions(client);
    }
    @Test void checkoutOptionsRenderAndInvalidMethodIsRejected() throws Exception {
        carts.add(customer,product.getId(),1);
        mvc.perform(get("/checkout").with(user(customer))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"SEPAY\"")));
        mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("paymentMethod","FAKE"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","paymentMethod"));
    }
}
