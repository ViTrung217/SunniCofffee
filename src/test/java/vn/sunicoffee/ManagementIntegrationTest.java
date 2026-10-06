package vn.sunicoffee;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserService userService;
    @Autowired AccountService accounts;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired CartService cart;
    @Autowired OrderService orderService;
    @Autowired CatalogService catalog;
    @Autowired InventoryService inventory;
    @Autowired StockMovementRepository movements;
    @Autowired ProductImageService images;
    @Autowired RevenueService revenue;
    @Autowired PasswordEncoder encoder;
    Product product;
    static final String CUSTOMER="customer@example.test", ADMIN="admin@example.test", OTHER="other@example.test";

    @BeforeEach void setup() {
        for(String table:new String[]{"order_events","stock_movements","product_images","order_items","customer_orders","cart_items","products","categories","app_users"}) jdbc.update("DELETE FROM "+table);
        userService.create("Customer",CUSTOMER,"TestPass!2026",Role.CUSTOMER);
        userService.create("Admin",ADMIN,"TestPass!2026",Role.ADMIN);
        userService.create("Other",OTHER,"TestPass!2026",Role.CUSTOMER);
        var c=new Category();c.setCode("COFFEE");c.setName("Cà phê");c.setDescription("Mẫu");c=categories.save(c);
        product=new Product();product.setSku("TEST-001");product.setName("Cà phê mẫu");product.setDescription("Dữ liệu kiểm thử");
        product.setPrice(new BigDecimal("95000"));product.setStock(10);product.setSalesUnit("túi");product.setActive(true);product.setCategory(c);product=products.save(product);
        inventory.record(product,null,null,StockMovementType.OPENING,10,"Tồn đầu kiểm thử",null);
    }
    AdminUserForm edit(String email,Role role,boolean enabled) {
        var u=accounts.byEmail(email);var f=new AdminUserForm();f.setFullName(u.getFullName());f.setRole(role);f.setEnabled(enabled);f.setVersion(u.getVersion());return f;
    }
    MockHttpSession login(String email) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("email",email).param("password","TestPass!2026"))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
    StockForm stock(StockMovementType type,int quantity) {
        var f=new StockForm();f.setProductId(product.getId());f.setType(type);f.setQuantity(quantity);f.setReason("Phiếu thử nghiệm");f.setRequestToken(UUID.randomUUID().toString());return f;
    }
    CustomerOrder buy(int qty) {
        cart.add(CUSTOMER,product.getId(),qty);var f=new CheckoutForm();f.setRecipientName("Người nhận");f.setPhone("0900000000");f.setAddress("Địa chỉ thử nghiệm");f.setCheckoutToken(UUID.randomUUID().toString());
        return orderService.checkout(CUSTOMER,f);
    }
    CustomerOrder complete(int qty,LocalDateTime at) {
        var o=buy(qty);orderService.transition(o.getId(),OrderStatus.CONFIRMED);orderService.transition(o.getId(),OrderStatus.SHIPPING,ADMIN,"Giao hàng mẫu","TEST-TRACK","");orderService.transition(o.getId(),OrderStatus.COMPLETED);
        assertThat(orders.findById(o.getId()).orElseThrow().getCompletedAt()).isNotNull();
        jdbc.update("UPDATE customer_orders SET completed_at=? WHERE id=?",at,o.getId());return o;
    }
    byte[] png() throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}

    @Test void managementScreensRenderIncludingEmptyRevenue() throws Exception {
        for(String path:new String[]{"/admin/users","/admin/users/new","/admin/users/"+accounts.byEmail(CUSTOMER).getId()+"/edit","/admin/inventory","/admin/revenue"})
            mvc.perform(get(path).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
        for(String path:new String[]{"/account","/account/password"}) mvc.perform(get(path).with(user(CUSTOMER))).andExpect(status().isOk());
    }
    @Test void profileUpdatesOwnInformationAndPrefillsCheckout() throws Exception {
        mvc.perform(post("/account/profile").with(user(CUSTOMER)).with(csrf()).param("fullName","Tên mới").param("phone","0901234567").param("address","Địa chỉ nhận hàng mới").param("role","ADMIN").param("email",OTHER))
            .andExpect(redirectedUrl("/account"));
        var u=accounts.byEmail(CUSTOMER);assertThat(u.getFullName()).isEqualTo("Tên mới");assertThat(u.getRole()).isEqualTo(Role.CUSTOMER);assertThat(accounts.byEmail(OTHER).getFullName()).isEqualTo("Other");
        cart.add(CUSTOMER,product.getId(),1);
        var f=(CheckoutForm)mvc.perform(get("/checkout").with(user(CUSTOMER))).andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("form");
        assertThat(f.getPhone()).isEqualTo("0901234567");assertThat(f.getAddress()).isEqualTo("Địa chỉ nhận hàng mới");
    }
    @Test void invalidProfileDoesNotSave() throws Exception {
        mvc.perform(post("/account/profile").with(user(CUSTOMER)).with(csrf()).param("fullName","").param("phone","invalid"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));assertThat(accounts.byEmail(CUSTOMER).getFullName()).isEqualTo("Customer");
    }
    @Test void passwordChangeLogsOutAllExistingSessionsAndUsesNewPassword() throws Exception {
        var first=login(CUSTOMER);var second=login(CUSTOMER);
        mvc.perform(post("/account/password").session(first).with(csrf()).param("currentPassword","TestPass!2026").param("password","Changed!2026").param("confirmPassword","Changed!2026"))
            .andExpect(redirectedUrl("/login?passwordChanged")).andExpect(unauthenticated());
        assertThat(encoder.matches("Changed!2026",accounts.byEmail(CUSTOMER).getPasswordHash())).isTrue();
        mvc.perform(get("/account").session(second)).andExpect(redirectedUrl("/login?expired")).andExpect(unauthenticated());
        mvc.perform(post("/login").with(csrf()).param("email",CUSTOMER).param("password","TestPass!2026")).andExpect(unauthenticated());
        mvc.perform(post("/login").with(csrf()).param("email",CUSTOMER).param("password","Changed!2026")).andExpect(authenticated());
    }
    @Test void passwordChangeRejectsWrongCurrentMismatchAndOversizedUtf8() throws Exception {
        for(String[] values:new String[][]{{"wrong","NewPass!2026","NewPass!2026"},{"TestPass!2026","NewPass!2026","Different!2026"},{"TestPass!2026","ấ".repeat(30),"ấ".repeat(30)}})
            mvc.perform(post("/account/password").with(user(CUSTOMER)).with(csrf()).param("currentPassword",values[0]).param("password",values[1]).param("confirmPassword",values[2]))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        assertThat(encoder.matches("TestPass!2026",accounts.byEmail(CUSTOMER).getPasswordHash())).isTrue();
    }
    @Test void lockedAccountCannotContinueOrLoginAndCanBeUnlocked() throws Exception {
        var session=login(CUSTOMER);var id=accounts.byEmail(CUSTOMER).getId();
        accounts.updateUser(ADMIN,id,edit(CUSTOMER,Role.CUSTOMER,false));
        mvc.perform(get("/account").session(session)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(post("/login").with(csrf()).param("email",CUSTOMER).param("password","TestPass!2026")).andExpect(unauthenticated());
        accounts.updateUser(ADMIN,id,edit(CUSTOMER,Role.CUSTOMER,true));login(CUSTOMER);
    }
    @Test void demotedAdminLosesExistingSession() throws Exception {
        userService.create("Second admin","second@example.test","TestPass!2026",Role.ADMIN);
        var session=login("second@example.test");var second=accounts.byEmail("second@example.test");
        accounts.updateUser(ADMIN,second.getId(),edit(second.getEmail(),Role.CUSTOMER,true));
        mvc.perform(get("/admin/users").session(session)).andExpect(redirectedUrl("/login?expired"));
        var fresh=login(second.getEmail());mvc.perform(get("/admin/users").session(fresh)).andExpect(status().isForbidden());
    }
    @Test void adminCannotDisableOrDemoteSelfAndStaleUpdateIsRejected() {
        var id=accounts.byEmail(ADMIN).getId();
        assertThatThrownBy(() -> accounts.updateUser(ADMIN,id,edit(ADMIN,Role.CUSTOMER,true))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> accounts.updateUser(ADMIN,id,edit(ADMIN,Role.ADMIN,false))).isInstanceOf(BusinessException.class);
        var f=edit(CUSTOMER,Role.CUSTOMER,false);var customerId=accounts.byEmail(CUSTOMER).getId();accounts.updateUser(ADMIN,customerId,f);
        assertThatThrownBy(() -> accounts.updateUser(ADMIN,customerId,f)).isInstanceOf(BusinessException.class);
        assertThat(users.countByRoleAndEnabledTrue(Role.ADMIN)).isEqualTo(1);
    }
    @Test void adminCreatesAccountButCustomerCannotAccessManagement() throws Exception {
        mvc.perform(post("/admin/users/new").with(user(ADMIN).roles("ADMIN")).with(csrf()).param("fullName","Nhân sự mới").param("email","new@example.test")
            .param("role","ADMIN").param("password","TestPass!2026").param("confirmPassword","TestPass!2026")).andExpect(redirectedUrl("/admin/users"));
        assertThat(accounts.byEmail("new@example.test").getRole()).isEqualTo(Role.ADMIN);
        for(String path:new String[]{"/admin/users","/admin/inventory","/admin/revenue"}) mvc.perform(get(path).with(user(CUSTOMER).roles("CUSTOMER"))).andExpect(status().isForbidden());
    }
    @Test void uploadValidImageReplaceAndRemove() throws Exception {
        var bytes=png();
        mvc.perform(multipart("/admin/products/"+product.getId()+"/image").file(new MockMultipartFile("image","../../photo.png","image/png",bytes)).with(user(ADMIN).roles("ADMIN")).with(csrf()))
            .andExpect(redirectedUrl("/admin/products/"+product.getId()+"/edit"));
        mvc.perform(get("/product-images/"+product.getId())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        assertThat(products.findById(product.getId()).orElseThrow().isHasImage()).isTrue();
        images.upload(product.getId(),new MockMultipartFile("image","replacement.png","image/png",bytes));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_images",Long.class)).isEqualTo(1);
        mvc.perform(get("/admin/products/"+product.getId()+"/edit").with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/admin/products/"+product.getId()+"/image/remove").with(user(ADMIN).roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/product-images/"+product.getId())).andExpect(status().isNotFound());assertThat(products.findById(product.getId()).orElseThrow().isHasImage()).isFalse();
    }
    @Test void rejectFakeOversizedAndHugeDimensionImagesWithoutLosingOldImage() throws Exception {
        images.upload(product.getId(),new MockMultipartFile("image","a.png","image/png",png()));var before=images.get(product.getId(),false).content();
        assertThatThrownBy(() -> images.upload(product.getId(),new MockMultipartFile("image","fake.png","image/png","<svg><script>bad</script></svg>".getBytes()))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> images.upload(product.getId(),new MockMultipartFile("image","large.jpg","image/jpeg",new byte[ProductImageService.MAX_BYTES+1]))).isInstanceOf(BusinessException.class);
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4097,1,BufferedImage.TYPE_INT_RGB),"png",out);
        assertThatThrownBy(() -> images.upload(product.getId(),new MockMultipartFile("image","wide.png","image/png",out.toByteArray()))).isInstanceOf(BusinessException.class);
        assertThat(images.get(product.getId(),false).content()).isEqualTo(before);
    }
    @Test void imageUploadRequiresAdminAndCsrfAndInactiveImageIsPrivate() throws Exception {
        var file=new MockMultipartFile("image","a.png","image/png",png());
        mvc.perform(multipart("/admin/products/"+product.getId()+"/image").file(file).with(user(CUSTOMER)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/products/"+product.getId()+"/image").file(file).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isForbidden());
        images.upload(product.getId(),file);catalog.archiveProduct(product.getId());mvc.perform(get("/product-images/"+product.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/product-images/"+product.getId()).with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test void receiptsAndIssuesProduceBalancedImmutableHistoryAndNoDuplicateReceipt() {
        var f=stock(StockMovementType.RECEIPT,5);inventory.manual(ADMIN,f);inventory.manual(ADMIN,f);
        inventory.manual(ADMIN,stock(StockMovementType.ISSUE,3));assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(12);
        assertThat(movements.count()).isEqualTo(3);assertThat(jdbc.queryForObject("SELECT SUM(quantity_delta) FROM stock_movements",Long.class)).isEqualTo(12);
        assertThat(movements.findByRequestToken(f.getRequestToken()).orElseThrow().getActor().getEmail()).isEqualTo(ADMIN);
    }
    @Test void inventoryRejectsNegativeStockAndForgedMovementType() {
        assertThatThrownBy(() -> inventory.manual(ADMIN,stock(StockMovementType.ISSUE,11))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> inventory.manual(ADMIN,stock(StockMovementType.RECEIPT,0))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> inventory.manual(ADMIN,stock(StockMovementType.SALE,1))).isInstanceOf(BusinessException.class);
        assertThat(movements.count()).isEqualTo(1);assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
    }
    @Test void inventoryHttpFlowAndInvalidSessionToken() throws Exception {
        var response=mvc.perform(get("/admin/inventory").with(user(ADMIN).roles("ADMIN"))).andReturn();var session=(MockHttpSession)response.getRequest().getSession();
        mvc.perform(post("/admin/inventory").session(session).with(user(ADMIN).roles("ADMIN")).with(csrf()).param("productId",product.getId().toString()).param("type","RECEIPT").param("quantity","3").param("reason","Nhập mới").param("requestToken",session.getAttribute("stockToken").toString()))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/inventory").session(session).with(user(ADMIN).roles("ADMIN")).with(csrf()).param("productId",product.getId().toString()).param("type","ISSUE").param("quantity","1").param("reason","Xuất").param("requestToken",UUID.randomUUID().toString()))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(13);
    }
    @Test void saleAndCancellationEachRecordExactlyOnce() {
        var o=buy(2);assertThat(movements.count()).isEqualTo(2);
        orderService.cancelByCustomer(o.getId(),CUSTOMER);assertThat(movements.count()).isEqualTo(3);
        assertThatThrownBy(() -> orderService.cancelByCustomer(o.getId(),CUSTOMER)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT SUM(quantity_delta) FROM stock_movements",Long.class)).isEqualTo(10);
    }
    @Test void concurrentIssuesCannotOverdrawInventory() throws Exception {
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> action=() -> {start.await();try {inventory.manual(ADMIN,stock(StockMovementType.ISSUE,8));return true;} catch(BusinessException e) {return false;}};
            var a=executor.submit(action);var b=executor.submit(action);start.countDown();assertThat((a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0)).isEqualTo(1);
        }
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(2);assertThat(movements.count()).isEqualTo(2);
    }
    @Test void productEditCannotBypassStockLedger() {
        var f=new ProductForm();f.setSku(product.getSku());f.setName(product.getName());f.setDescription(product.getDescription());f.setPrice(product.getPrice());f.setStock(999);f.setSalesUnit("túi");f.setActive(true);f.setCategoryId(product.getCategory().getId());f.setVersion(product.getVersion());
        assertThatThrownBy(() -> catalog.saveProduct(product.getId(),f)).isInstanceOf(BusinessException.class);assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
    }
    @Test void revenueUsesCompletionDateAndExcludesPendingCanceledAndOutsideDates() throws Exception {
        complete(2,LocalDateTime.of(2026,10,2,0,0));complete(1,LocalDateTime.of(2026,10,2,23,59,59));complete(1,LocalDateTime.of(2026,10,3,0,0));
        buy(1);var canceled=buy(1);orderService.cancelByCustomer(canceled.getId(),CUSTOMER);
        var report=revenue.report(LocalDate.of(2026,10,2),LocalDate.of(2026,10,2));
        assertThat(report.revenue()).isEqualByComparingTo("285000");assertThat(report.orderCount()).isEqualTo(2);assertThat(report.units()).isEqualTo(3);assertThat(report.average()).isEqualByComparingTo("142500");
        assertThat(report.top()).hasSize(1);assertThat(report.top().getFirst().quantity()).isEqualTo(3);
        mvc.perform(get("/admin/revenue").with(user(ADMIN).roles("ADMIN")).param("from","2026-10-02").param("to","2026-10-02")).andExpect(status().isOk());
    }
    @Test void revenuePreservesSoldPriceAndFlagsLegacyUndatedOrders() {
        var legacy=complete(1,LocalDateTime.of(2026,10,2,10,0));jdbc.update("UPDATE customer_orders SET completed_at=NULL WHERE id=?",legacy.getId());
        complete(2,LocalDateTime.of(2026,10,2,10,0));var p=products.findById(product.getId()).orElseThrow();p.setPrice(new BigDecimal("1000000"));products.save(p);
        var report=revenue.report(LocalDate.of(2026,10,2),LocalDate.of(2026,10,2));assertThat(report.undatedCompleted()).isEqualTo(1);assertThat(report.revenue()).isEqualByComparingTo("190000");
        assertThat(report.top().getFirst().revenue()).isEqualByComparingTo("190000");
    }
    @Test void revenueRejectsInvalidRangesAndShowsZeroData() throws Exception {
        assertThatThrownBy(() -> revenue.report(LocalDate.of(2026,10,3),LocalDate.of(2026,10,2))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> revenue.report(LocalDate.of(2020,1,1),LocalDate.of(2026,10,2))).isInstanceOf(BusinessException.class);
        var empty=revenue.report(LocalDate.of(2026,10,1),LocalDate.of(2026,10,3));assertThat(empty.daily()).hasSize(3);assertThat(empty.revenue()).isZero();
        mvc.perform(get("/admin/revenue").with(user(ADMIN).roles("ADMIN")).param("from","2026-10-03").param("to","2026-10-02")).andExpect(status().isOk()).andExpect(model().attributeExists("error"));
    }
}
