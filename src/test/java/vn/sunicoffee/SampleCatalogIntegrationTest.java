package vn.sunicoffee;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.core.io.ClassPathResource;
import vn.sunicoffee.domain.Product;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.SampleCatalogService;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SampleCatalogIntegrationTest {
    @Autowired SampleCatalogService importer;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @BeforeEach void clear() {
        for (String table:new String[]{"order_events","stock_movements","product_images","order_items","customer_orders","cart_items","products","categories","app_users"}) jdbc.update("DELETE FROM "+table);
    }
    @Test void freshImportCreatesAllProductsWithDescriptionsImagesAndOpeningLedger() throws Exception {
        assertThat(importer.refresh()).isEqualTo(new SampleCatalogService.ImportResult(36,0));
        assertThat(categories.count()).isEqualTo(4);
        for (var p:products.findAll()) {
            assertThat(p.getDescription()).contains("Quy cách", "Gợi ý sử dụng", "Nguồn dữ liệu");
            assertThat(p.getDescription().length()).isLessThanOrEqualTo(2000);
            assertThat(new ClassPathResource("static"+p.getImageUrl()).exists()).isTrue();
            assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(quantity_delta),0) FROM stock_movements WHERE product_id=?",Integer.class,p.getId())).isEqualTo(p.getStock());
        }
    }
    @Test void refreshChangesOnlySampleDescriptionsAndAddsNoDuplicateStock() throws Exception {
        importer.refresh();
        var p=products.findBySku("DEMO-CF-001").orElseThrow(); long id=p.getId();
        p.setDescription("Mô tả cũ"); p.setName("Tên đã chỉnh"); p.setPrice(new BigDecimal("123456")); p.setStock(7);p.setActive(false);products.saveAndFlush(p);
        var other=new Product();other.setSku("STORE-CUSTOM");other.setName("Hàng nhập riêng");other.setDescription("Mô tả của cửa hàng");other.setPrice(new BigDecimal("100000"));other.setStock(0);other.setActive(true);other.setSalesUnit("túi");other.setCategory(p.getCategory());products.saveAndFlush(other);
        int movements=jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements",Integer.class);
        assertThat(importer.refresh()).isEqualTo(new SampleCatalogService.ImportResult(0,1));
        assertThat(importer.refresh()).isEqualTo(new SampleCatalogService.ImportResult(0,0));
        var after=products.findById(id).orElseThrow();
        assertThat(after.getName()).isEqualTo("Tên đã chỉnh");assertThat(after.getPrice()).isEqualByComparingTo("123456");
        assertThat(after.getStock()).isEqualTo(7);assertThat(after.isActive()).isFalse();
        assertThat(other.getDescription()).isEqualTo("Mô tả của cửa hàng");
        assertThat(products.count()).isEqualTo(37);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements",Integer.class)).isEqualTo(movements);
    }
    @Test void publicCatalogueShowsNewItemsAndFullMultilineDescription() throws Exception {
        importer.refresh();
        var p=products.findBySku("DEMO-BR-004").orElseThrow();
        mvc.perform(get("/products").param("q","French press")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Bình French press 350 ml")));
        mvc.perform(get("/products/"+p.getId())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Thông tin sản phẩm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Quy cách và thông tin:")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguồn dữ liệu:")));
    }
}
