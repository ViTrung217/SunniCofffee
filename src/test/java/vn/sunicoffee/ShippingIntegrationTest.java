package vn.sunicoffee;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties="app.shipping.fee=25000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShippingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserService users;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired CartService cart;
    @MockitoSpyBean OrderService service;
    @Autowired OrderHistoryService history;
    @Autowired RevenueService revenue;
    Product product;
    static final String CUSTOMER="shipping@example.test", ADMIN="shipping-admin@example.test", OTHER="other-shipping@example.test";
    @BeforeEach void setup() {
        for(String table:new String[]{"order_events","stock_movements","product_images","order_items","customer_orders","cart_items","products","categories","app_users"}) jdbc.update("DELETE FROM "+table);
        users.create("Customer",CUSTOMER,"TestPass!2026",Role.CUSTOMER);
        users.create("Admin",ADMIN,"TestPass!2026",Role.ADMIN);
        users.create("Other",OTHER,"TestPass!2026",Role.CUSTOMER);
        var c=new Category(); c.setCode("SHIP"); c.setName("Giao hàng"); c.setDescription("Mẫu"); c=categories.save(c);
        product=new Product(); product.setSku("SHIP-01"); product.setName("Cà phê giao hàng"); product.setDescription("Mẫu");
        product.setPrice(new BigDecimal("95000")); product.setStock(10); product.setSalesUnit("túi"); product.setActive(true); product.setCategory(c); product=products.save(product);
    }
    CheckoutForm form() {
        var f=new CheckoutForm(); f.setRecipientName("Người nhận"); f.setPhone("0900000000"); f.setAddress("Địa chỉ nhận hàng mẫu"); f.setCheckoutToken(UUID.randomUUID().toString()); return f;
    }
    CustomerOrder buy(int qty) {cart.add(CUSTOMER,product.getId(),qty);return service.checkout(CUSTOMER,form());}
    void move(CustomerOrder o,OrderStatus next) { service.transition(o.getId(),next,ADMIN,"Hãng mẫu","TRACK-01","Ghi chú kiểm thử"); }
    void ship(CustomerOrder o) {move(o,OrderStatus.CONFIRMED);move(o,OrderStatus.SHIPPING);}
    CustomerOrder current(CustomerOrder o) {return service.detail(o.getId(),CUSTOMER,false);}
    @Test void feeIsStoredAndRevenueExcludesShipping() throws Exception {
        var o=buy(2); assertThat(o.getTotal()).isEqualByComparingTo("215000"); assertThat(o.getShippingFee()).isEqualByComparingTo("25000");
        ship(o);move(o,OrderStatus.COMPLETED);
        var today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")); assertThat(revenue.report(today,today).revenue()).isEqualByComparingTo("190000");
        mvc.perform(get("/orders/"+o.getId()).with(user(CUSTOMER))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("25.000")));
    }
    @Test void shipmentRequiresTrackingAndFailureRequiresReason() {
        var o=buy(1); move(o,OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> service.transition(o.getId(),OrderStatus.SHIPPING,ADMIN)).isInstanceOf(BusinessException.class);
        move(o,OrderStatus.SHIPPING);
        assertThatThrownBy(() -> service.transition(o.getId(),OrderStatus.DELIVERY_FAILED,ADMIN)).isInstanceOf(BusinessException.class);
        assertThat(current(o).getStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(current(o).getCarrier()).isEqualTo("Hãng mẫu");
    }
    @Test void failedDeliveryCanRetryWithoutDeductingAgain() {
        var o=buy(2);ship(o);move(o,OrderStatus.DELIVERY_FAILED);move(o,OrderStatus.SHIPPING);move(o,OrderStatus.COMPLETED);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThat(history.events(o.getId())).hasSize(6);
        assertThat(current(o).getCompletedAt()).isNotNull();
    }
    @Test void receiveReturnRestocksOnlySaleableQuantityAndCannotRepeat() throws Exception {
        var o=buy(3);ship(o);move(o,OrderStatus.DELIVERY_FAILED);move(o,OrderStatus.RETURNING);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(7);
        mvc.perform(get("/admin/orders/"+o.getId()).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
        Long line=current(o).getItems().getFirst().getId();
        service.receiveReturn(o.getId(),ADMIN,Map.of(line,2),"Một túi bị hỏng");
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(9);
        assertThat(current(o).getItems().getFirst().getRestockedQuantity()).isEqualTo(2);
        assertThat(current(o).getReturnedAt()).isNotNull();
        assertThatThrownBy(() -> service.receiveReturn(o.getId(),ADMIN,Map.of(line,2),"Lặp")).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE type='DELIVERY_RETURN'",Long.class)).isEqualTo(1);
        mvc.perform(get("/orders/"+o.getId()).with(user(CUSTOMER))).andExpect(status().isOk());
    }
    @Test void invalidReturnDoesNotChangeStockOrHistory() {
        var o=buy(2);ship(o);move(o,OrderStatus.DELIVERY_FAILED);move(o,OrderStatus.RETURNING);
        Long line=current(o).getItems().getFirst().getId();int count=history.events(o.getId()).size();
        for(var quantities:List.of(Map.of(line,3),Map.of(line,-1),Map.of(line+999,1)))
            assertThatThrownBy(() -> service.receiveReturn(o.getId(),ADMIN,quantities,"Kiểm tra")).isInstanceOf(BusinessException.class);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThat(history.events(o.getId())).hasSize(count);
        assertThat(current(o).getStatus()).isEqualTo(OrderStatus.RETURNING);
    }
    @Test void codSettlementIsSeparateAndIdempotent() {
        var o=buy(1);
        assertThatThrownBy(() -> service.settleCod(o.getId(),ADMIN)).isInstanceOf(BusinessException.class);
        ship(o);move(o,OrderStatus.COMPLETED);assertThat(current(o).getCodSettledAt()).isNull();
        service.settleCod(o.getId(),ADMIN);var settled=current(o).getCodSettledAt();
        service.settleCod(o.getId(),ADMIN);assertThat(current(o).getCodSettledAt()).isEqualTo(settled);
        assertThat(history.events(o.getId())).hasSize(5);
    }
    @Test void topProductsRemainGroupedAfterRenameAndSkuChange() {
        var first=buy(1);ship(first);move(first,OrderStatus.COMPLETED);
        var p=products.findById(product.getId()).orElseThrow();p.setName("Tên mới");p.setSku("NEW-SKU");p.setPrice(new BigDecimal("100000"));products.save(p);
        var second=buy(2);ship(second);move(second,OrderStatus.COMPLETED);
        var today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));var top=revenue.report(today,today).top();
        assertThat(top).singleElement().satisfies(t -> {assertThat(t.quantity()).isEqualTo(3);assertThat(t.revenue()).isEqualByComparingTo("295000");assertThat(t.name()).isEqualTo("Tên mới");});
        assertThat(current(first).getItems().getFirst().getProductName()).isEqualTo("Cà phê giao hàng");
    }
    @Test void checkoutTabsAndReplayRemainValid() throws Exception {
        cart.add(CUSTOMER,product.getId(),1);
        var first=mvc.perform(get("/checkout").with(user(CUSTOMER))).andReturn();var session=(MockHttpSession)first.getRequest().getSession();
        String token=session.getAttribute("checkoutToken").toString();
        mvc.perform(get("/checkout").session(session).with(user(CUSTOMER))).andExpect(status().isOk());
        for(int attempt=0;attempt<2;attempt++) mvc.perform(post("/checkout").session(session).with(user(CUSTOMER)).with(csrf())
            .param("checkoutToken",token).param("recipientName","Test").param("phone","0900000000").param("address","Địa chỉ thử nghiệm"))
            .andExpect(status().is3xxRedirection());
        assertThat(orders.count()).isEqualTo(1);
    }
    @Test void lockFailureGivesRetryMessageAndKeepsCart() throws Exception {
        cart.add(CUSTOMER,product.getId(),1);
        var response=mvc.perform(get("/checkout").with(user(CUSTOMER))).andReturn();var session=(MockHttpSession)response.getRequest().getSession();
        org.mockito.Mockito.doThrow(new org.springframework.dao.CannotAcquireLockException("test contention"))
            .when(service).checkout(org.mockito.ArgumentMatchers.eq(CUSTOMER),org.mockito.ArgumentMatchers.any());
        mvc.perform(post("/checkout").session(session).with(user(CUSTOMER)).with(csrf()).param("checkoutToken",session.getAttribute("checkoutToken").toString())
            .param("recipientName","Test").param("phone","0900000000").param("address","Địa chỉ thử nghiệm"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng thử đặt hàng lại")));
        assertThat(cart.items(CUSTOMER)).hasSize(1);assertThat(orders.count()).isZero();
    }
    @Test void adminsCanFilterOrdersAndFindOverduePending() throws Exception {
        var o=buy(1);jdbc.update("UPDATE customer_orders SET created_at=? WHERE id=?",LocalDateTime.now().minusDays(4),o.getId());
        mvc.perform(get("/admin/orders").with(user(ADMIN).roles("ADMIN")).param("q","SUNI-"+o.getId()).param("overdue","true"))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("SUNI-"+o.getId())));
        mvc.perform(get("/admin/orders").with(user(ADMIN).roles("ADMIN")).param("status","RETURNED"))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có đơn hàng")));
    }
    @Test void customerCannotMutateShippingOrReadOthersHistory() throws Exception {
        var o=buy(1);
        for(String action:List.of("status","return","cod")) mvc.perform(post("/admin/orders/"+o.getId()+"/"+action).with(user(CUSTOMER)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/orders/"+o.getId()).with(user(OTHER))).andExpect(status().isNotFound());
        mvc.perform(post("/admin/orders/"+o.getId()+"/cod").with(user(ADMIN).roles("ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void simultaneousCancellationsReturnStockOnce() throws Exception {
        var o=buy(2);var start=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> task=() -> {start.await(10,TimeUnit.SECONDS);try {service.cancelByCustomer(o.getId(),CUSTOMER);return true;}catch(BusinessException e){return false;}};
            var a=pool.submit(task);var b=pool.submit(task);
            assertThat((a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0)).isEqualTo(1);
        }
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE type='CANCEL_RETURN'",Long.class)).isEqualTo(1);
    }
    @Autowired InventoryService inventory;
    @Test void checkoutAndManualIssueCannotBothConsumeLastStock() throws Exception {
        var p=products.findById(product.getId()).orElseThrow();p.setStock(2);products.save(p);
        cart.add(CUSTOMER,product.getId(),2);var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var checkout=pool.submit(() -> {barrier.await(10,TimeUnit.SECONDS);try{service.checkout(CUSTOMER,form());return true;}catch(BusinessException e){return false;}});
            var issue=pool.submit(() -> {barrier.await(10,TimeUnit.SECONDS);var f=new StockForm();f.setProductId(product.getId());f.setQuantity(2);f.setType(StockMovementType.ISSUE);f.setReason("Hao hụt mẫu");f.setRequestToken(UUID.randomUUID().toString());try{inventory.manual(ADMIN,f);return true;}catch(BusinessException e){return false;}});
            assertThat((checkout.get(15,TimeUnit.SECONDS)?1:0)+(issue.get(15,TimeUnit.SECONDS)?1:0)).isEqualTo(1);
        }
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();
        assertThat(jdbc.queryForObject("SELECT SUM(quantity_delta) FROM stock_movements",Integer.class)).isEqualTo(-2);
    }
    @Test void concurrentCancellationAndCheckoutPreserveBalance() throws Exception {
        var old=buy(2);cart.add(OTHER,product.getId(),1);var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var cancel=pool.submit(() -> {barrier.await(10,TimeUnit.SECONDS);service.cancelByCustomer(old.getId(),CUSTOMER);return true;});
            var buy=pool.submit(() -> {barrier.await(10,TimeUnit.SECONDS);return service.checkout(OTHER,form());});
            assertThat(cancel.get(15,TimeUnit.SECONDS)).isTrue();assertThat(buy.get(15,TimeUnit.SECONDS).getId()).isNotNull();
        }
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT SUM(quantity_delta) FROM stock_movements",Integer.class)).isEqualTo(-1);
    }
    @Test void reversedCartInsertionOrderStillLocksProductsConsistently() throws Exception {
        var second=new Product();second.setSku("SHIP-02");second.setName("Sản phẩm thứ hai");second.setDescription("Mẫu");second.setPrice(BigDecimal.TEN);second.setStock(1);second.setSalesUnit("chiếc");second.setActive(true);second.setCategory(product.getCategory());second=products.save(second);
        var p=products.findById(product.getId()).orElseThrow();p.setStock(1);products.save(p);
        cart.add(CUSTOMER,second.getId(),1);cart.add(CUSTOMER,product.getId(),1);
        cart.add(OTHER,product.getId(),1);cart.add(OTHER,second.getId(),1);
        var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            java.util.function.Function<String,Callable<Boolean>> action=email -> () -> {barrier.await(10,TimeUnit.SECONDS);try{service.checkout(email,form());return true;}catch(BusinessException e){return false;}};
            var a=pool.submit(action.apply(CUSTOMER));var b=pool.submit(action.apply(OTHER));
            assertThat((a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0)).isEqualTo(1);
        }
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();
        assertThat(products.findById(second.getId()).orElseThrow().getStock()).isZero();
        assertThat(orders.count()).isEqualTo(1);
    }

}
