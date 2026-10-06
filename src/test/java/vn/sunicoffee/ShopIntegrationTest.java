package vn.sunicoffee;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.*;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired CartService cart;
    @Autowired OrderService orderService;
    @Autowired CatalogService catalog;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;
    Product product;
    Category category;
    static final String CUSTOMER="customer@example.test";
    static final String OTHER="other@example.test";
    static final String ADMIN="admin@example.test";

    @BeforeEach void setup() {
        for(String table:new String[]{"order_events","stock_movements","product_images","order_items","customer_orders","cart_items","products","categories","app_users"}) jdbc.update("DELETE FROM "+table);
        userService.create("Customer",CUSTOMER,"TestPass!2026",Role.CUSTOMER);
        userService.create("Other",OTHER,"TestPass!2026",Role.CUSTOMER);
        userService.create("Admin",ADMIN,"TestPass!2026",Role.ADMIN);
        category=new Category(); category.setCode("COFFEE"); category.setName("Cà phê"); category.setDescription("Mẫu"); category=categories.save(category);
        product=new Product(); product.setSku("TEST-001"); product.setName("Cà phê mẫu"); product.setDescription("Dữ liệu kiểm thử");
        product.setPrice(new BigDecimal("95000")); product.setStock(10); product.setSalesUnit("túi"); product.setActive(true); product.setCategory(category); product=products.save(product);
    }
    CheckoutForm form() {
        CheckoutForm f=new CheckoutForm(); f.setRecipientName("Người nhận mẫu"); f.setPhone("0900000000"); f.setAddress("Địa chỉ thử nghiệm tại Hà Nội");
        f.setCheckoutToken(UUID.randomUUID().toString()); return f;
    }
    CustomerOrder buy() { cart.add(CUSTOMER,product.getId(),2); return orderService.checkout(CUSTOMER,form()); }

    @Test void publicPagesAndApiRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Cà phê mẫu")));
        mvc.perform(get("/products/"+product.getId())).andExpect(status().isOk());
        mvc.perform(get("/products").param("q","notfound")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk()); mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.items[0].price").value(95000));
    }
    @Test void registrationAlwaysCreatesCustomerAndHashesPassword() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("fullName","New User").param("email","New@Example.test")
                .param("password","NewPass!2026").param("confirmPassword","NewPass!2026").param("role","ADMIN"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));
        var u=users.findByEmail("new@example.test").orElseThrow(); assertThat(u.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(u.getPasswordHash()).isNotEqualTo("NewPass!2026"); assertThat(encoder.matches("NewPass!2026",u.getPasswordHash())).isTrue();
    }
    @Test void registrationRejectsInvalidAndDuplicateData() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("fullName","").param("email","bad").param("password","short").param("confirmPassword","x"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        mvc.perform(post("/register").with(csrf()).param("fullName","Duplicate").param("email",CUSTOMER).param("password","TestPass!2026").param("confirmPassword","TestPass!2026"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        assertThat(users.count()).isEqualTo(3);
    }
    @Test void actualFormLoginAcceptsOnlyCorrectPassword() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("email",CUSTOMER).param("password","TestPass!2026")).andExpect(authenticated().withUsername(CUSTOMER));
        mvc.perform(post("/login").with(csrf()).param("email",CUSTOMER).param("password","wrong")).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }
    @Test void adminRoutesRequireAdminAndMutationsRequireCsrf() throws Exception {
        mvc.perform(get("/admin/products")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/products").with(user(CUSTOMER).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/cart/add").with(user(CUSTOMER)).param("productId",product.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/products/"+product.getId()+"/archive").with(user(CUSTOMER)).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void allAdminAndCustomerScreensRender() throws Exception {
        for(String path:new String[]{"/admin/products","/admin/products/new","/admin/products/"+product.getId()+"/edit","/admin/categories","/admin/categories/new","/admin/categories/"+category.getId()+"/edit","/admin/orders"})
            mvc.perform(get(path).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/cart").with(user(CUSTOMER))).andExpect(status().isOk());
        cart.add(CUSTOMER,product.getId(),1);
        mvc.perform(get("/cart").with(user(CUSTOMER))).andExpect(status().isOk());
        mvc.perform(get("/checkout").with(user(CUSTOMER))).andExpect(status().isOk());
        var o=orderService.checkout(CUSTOMER,form());
        for(String path:new String[]{"/orders","/orders/"+o.getId()}) mvc.perform(get(path).with(user(CUSTOMER))).andExpect(status().isOk());
        mvc.perform(get("/admin/orders/"+o.getId()).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test void adminCanCreateAndValidateCatalog() throws Exception {
        mvc.perform(post("/admin/categories/save").with(user(ADMIN).roles("ADMIN")).with(csrf()).param("code","TOOLS").param("name","Dụng cụ").param("description","Mẫu"))
            .andExpect(redirectedUrl("/admin/categories"));
        mvc.perform(post("/admin/products/save").with(user(ADMIN).roles("ADMIN")).with(csrf()).param("sku","NEW-01").param("name","Phin mẫu")
            .param("description","Sản phẩm mới").param("price","50000").param("stock","3").param("salesUnit","chiếc").param("active","true").param("categoryId",category.getId().toString()))
            .andExpect(redirectedUrl("/admin/products"));
        assertThat(products.findBySku("NEW-01")).isPresent();
        mvc.perform(post("/admin/products/save").with(user(ADMIN).roles("ADMIN")).with(csrf()).param("sku","BAD").param("price","-1").param("stock","-1"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
    }
    @Test void categoryInUseCannotBeDeleted() {
        assertThatThrownBy(() -> catalog.deleteCategory(category.getId())).isInstanceOf(BusinessException.class);
        assertThat(categories.existsById(category.getId())).isTrue();
    }
    @Test void archivedProductIsHiddenAndCannotBePurchased() throws Exception {
        catalog.archiveProduct(product.getId());
        mvc.perform(get("/products/"+product.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/"+product.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products")).andExpect(jsonPath("$.totalElements").value(0));
        assertThatThrownBy(() -> cart.add(CUSTOMER,product.getId(),1)).isInstanceOf(BusinessException.class);
    }
    @Test void cartEnforcesQuantityAndAccountOwnership() {
        cart.add(CUSTOMER,product.getId(),2); cart.update(CUSTOMER,product.getId(),3);
        assertThat(cart.items(CUSTOMER).getFirst().getQuantity()).isEqualTo(3);
        assertThatThrownBy(() -> cart.add(CUSTOMER,product.getId(),99)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> cart.update(CUSTOMER,product.getId(),0)).isInstanceOf(BusinessException.class);
        cart.remove(OTHER,product.getId()); assertThat(cart.items(CUSTOMER)).hasSize(1);
        cart.remove(CUSTOMER,product.getId()); assertThat(cart.items(CUSTOMER)).isEmpty();
    }
    @Test void checkoutUsesCurrentDatabasePriceAndIsIdempotent() {
        cart.add(CUSTOMER,product.getId(),2); CheckoutForm f=form();
        var p=products.findById(product.getId()).orElseThrow(); p.setPrice(new BigDecimal("100000")); products.save(p);
        var o=orderService.checkout(CUSTOMER,f); var retry=orderService.checkout(CUSTOMER,f);
        assertThat(o.getId()).isEqualTo(retry.getId()); assertThat(orders.count()).isEqualTo(1);
        assertThat(o.getTotal()).isEqualByComparingTo("200000"); assertThat(cart.items(CUSTOMER)).isEmpty();
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
        p=products.findById(product.getId()).orElseThrow(); p.setName("Tên mới"); p.setPrice(new BigDecimal("200000")); products.save(p);
        var saved=orderService.detail(o.getId(),CUSTOMER,false);
        assertThat(saved.getItems().getFirst().getProductName()).isEqualTo("Cà phê mẫu");
        assertThat(saved.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("100000");
    }
    @Test void checkoutRejectsEmptyCartAndStaleStockWithoutPartialOrder() {
        assertThatThrownBy(() -> orderService.checkout(CUSTOMER,form())).isInstanceOf(BusinessException.class);
        cart.add(CUSTOMER,product.getId(),3);
        var p=products.findById(product.getId()).orElseThrow(); p.setStock(1); products.save(p);
        assertThatThrownBy(() -> orderService.checkout(CUSTOMER,form())).isInstanceOf(BusinessException.class);
        assertThat(orders.count()).isZero(); assertThat(cart.items(CUSTOMER)).hasSize(1);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(1);
    }
    @Test void checkoutRejectsForgedSessionToken() throws Exception {
        cart.add(CUSTOMER,product.getId(),1);
        mvc.perform(post("/checkout").with(user(CUSTOMER)).with(csrf()).param("checkoutToken",UUID.randomUUID().toString())
            .param("recipientName","Test").param("phone","0900000000").param("address","Địa chỉ thử nghiệm"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        assertThat(orders.count()).isZero();
    }
    @Test void checkoutHttpFlowCreatesOrder() throws Exception {
        cart.add(CUSTOMER,product.getId(),1);
        var response=mvc.perform(get("/checkout").with(user(CUSTOMER))).andReturn();
        var session=(MockHttpSession)response.getRequest().getSession();
        mvc.perform(post("/checkout").session(session).with(user(CUSTOMER)).with(csrf())
            .param("checkoutToken",session.getAttribute("checkoutToken").toString()).param("recipientName","Test").param("phone","0900000000").param("address","Địa chỉ thử nghiệm"))
            .andExpect(status().is3xxRedirection());
        assertThat(orders.count()).isEqualTo(1);
    }
    @Test void customerCannotReadOrCancelAnotherCustomersOrder() throws Exception {
        var o=buy();
        mvc.perform(get("/orders/"+o.getId()).with(user(OTHER))).andExpect(status().isNotFound());
        mvc.perform(post("/orders/"+o.getId()+"/cancel").with(user(OTHER)).with(csrf())).andExpect(status().isNotFound());
        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
    @Test void cancelRestoresInventoryOnlyOnce() {
        var o=buy(); orderService.cancelByCustomer(o.getId(),CUSTOMER);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThatThrownBy(() -> orderService.cancelByCustomer(o.getId(),CUSTOMER)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> orderService.transition(o.getId(),OrderStatus.CANCELLED)).isInstanceOf(BusinessException.class);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
    }
    @Test void orderStatusesFollowBusinessRules() {
        var o=buy();
        assertThatThrownBy(() -> orderService.transition(o.getId(),OrderStatus.COMPLETED)).isInstanceOf(BusinessException.class);
        orderService.transition(o.getId(),OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> orderService.cancelByCustomer(o.getId(),CUSTOMER)).isInstanceOf(BusinessException.class);
        orderService.transition(o.getId(),OrderStatus.SHIPPING,ADMIN,"Giao hàng mẫu","TEST-TRACK",""); orderService.transition(o.getId(),OrderStatus.COMPLETED);
        assertThatThrownBy(() -> orderService.transition(o.getId(),OrderStatus.CANCELLED)).isInstanceOf(BusinessException.class);
    }
    @Test void adminCannotOverwriteInventoryFromStaleEditForm() {
        var f=new ProductForm(); f.setSku(product.getSku()); f.setName(product.getName()); f.setDescription(product.getDescription());
        f.setPrice(product.getPrice()); f.setStock(10); f.setSalesUnit("túi"); f.setActive(true); f.setCategoryId(category.getId()); f.setVersion(product.getVersion());
        buy(); assertThatThrownBy(() -> catalog.saveProduct(product.getId(),f)).isInstanceOf(BusinessException.class);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
    }
    @Test void concurrentCheckoutsCannotOversellLastItem() throws Exception {
        var p=products.findById(product.getId()).orElseThrow(); p.setStock(1); products.save(p);
        cart.add(CUSTOMER,product.getId(),1); cart.add(OTHER,product.getId(),1);
        CyclicBarrier ready=new CyclicBarrier(2);
        boolean customerSucceeded;
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(() -> checkoutWithPreloadedStock(ready,CUSTOMER));
            var b=executor.submit(() -> checkoutWithPreloadedStock(ready,OTHER));
            customerSucceeded=a.get(15,TimeUnit.SECONDS);
            assertThat((customerSucceeded ? 1 : 0)+(b.get(15,TimeUnit.SECONDS) ? 1 : 0)).isEqualTo(1);
        }
        assertThat(orders.count()).isEqualTo(1); assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();
        String winner=customerSucceeded ? CUSTOMER : OTHER;
        String loser=customerSucceeded ? OTHER : CUSTOMER;
        assertThat(cart.items(winner)).isEmpty();
        assertThat(cart.items(loser)).singleElement().satisfies(line -> assertThat(line.getQuantity()).isEqualTo(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_items",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE type='SALE'",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT quantity_delta FROM stock_movements WHERE type='SALE'",Integer.class)).isEqualTo(-1);
        assertThat(jdbc.queryForObject("SELECT balance_after FROM stock_movements WHERE type='SALE'",Integer.class)).isZero();
    }
    private boolean checkoutWithPreloadedStock(CyclicBarrier ready,String email) {
        try {
            return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status -> {
                // Both transactions must read stock=1 before either checks out. This also
                // verifies that checkout refreshes an already managed, stale product.
                assertThat(cart.items(email).getFirst().getProduct().getStock()).isEqualTo(1);
                try { ready.await(10,TimeUnit.SECONDS); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                catch(BrokenBarrierException | TimeoutException e) { throw new IllegalStateException(e); }
                orderService.checkout(email,form());
                return true;
            }));
        } catch(BusinessException e) {
            assertThat(e).hasMessageContaining("không còn đủ hàng");
            return false;
        }
    }
    @Test void checkoutShowsStockErrorWhenAnotherCustomerBoughtLastItem() throws Exception {
        var p=products.findById(product.getId()).orElseThrow(); p.setStock(1); products.save(p);
        cart.add(CUSTOMER,product.getId(),1); cart.add(OTHER,product.getId(),1);
        var response=mvc.perform(get("/checkout").with(user(OTHER))).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)response.getRequest().getSession();
        orderService.checkout(CUSTOMER,form());
        mvc.perform(post("/checkout").session(session).with(user(OTHER)).with(csrf())
            .param("checkoutToken",session.getAttribute("checkoutToken").toString()).param("recipientName","Test")
            .param("phone","0900000000").param("address","Địa chỉ thử nghiệm"))
            .andExpect(status().isOk()).andExpect(view().name("store/checkout"))
            .andExpect(model().attributeHasErrors("form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("không còn đủ hàng")));
        assertThat(orders.count()).isEqualTo(1);
        assertThat(cart.items(OTHER)).hasSize(1);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();
    }
    @Test void shortageOnLaterCartLineRollsBackEarlierStockDeduction() {
        var last=new Product(); last.setSku("TEST-LAST"); last.setName("Sản phẩm cuối"); last.setDescription("Mẫu");
        last.setPrice(BigDecimal.TEN); last.setStock(1); last.setSalesUnit("chiếc"); last.setActive(true);
        last.setCategory(category); last=products.save(last);
        cart.add(CUSTOMER,product.getId(),2); cart.add(CUSTOMER,last.getId(),1);
        cart.add(OTHER,last.getId(),1); orderService.checkout(OTHER,form());

        assertThatThrownBy(() -> orderService.checkout(CUSTOMER,form()))
            .isInstanceOf(BusinessException.class).hasMessageContaining("không còn đủ hàng");
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThat(products.findById(last.getId()).orElseThrow().getStock()).isZero();
        assertThat(cart.items(CUSTOMER)).hasSize(2);
        assertThat(orders.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements",Long.class)).isEqualTo(1);
    }
}
