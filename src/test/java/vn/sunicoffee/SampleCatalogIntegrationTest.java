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
    @Autowired vn.sunicoffee.service.ProductSearchService searchService;
    @Test void searchMatchesCategoryAccentsWordOrderAndTypingVariant() throws Exception {
        importer.refresh();
        for(String query:new String[]{"máy pha cà phê","may pha ca phe","máy pha cà phee","  MÁY   PHA  ","cà phê máy pha"}) {
            var result=searchService.search(query,null,0);
            assertThat(result.getTotalElements()).as(query).isEqualTo(6);
            assertThat(result.getContent()).allMatch(p->p.getSku().startsWith("DEMO-MC-"));
        }
        mvc.perform(get("/products").param("q","máy pha cà phê")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Máy pha cầm tay Suni A")));
        mvc.perform(get("/api/products").param("q","may pha ca phe")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(6));
    }
    @Test void searchKeepsCategoryVisibilityAndAllQueryWords() throws Exception {
        importer.refresh();
        var hidden=products.findBySku("DEMO-MC-001").orElseThrow();hidden.setActive(false);products.saveAndFlush(hidden);
        assertThat(searchService.search("máy pha cà phê",null,0).getTotalElements()).isEqualTo(5);
        Long coffee=categories.findByCode("COFFEE").orElseThrow().getId();
        assertThat(searchService.search("máy pha cà phê",coffee,0)).isEmpty();
        assertThat(searchService.search("máy pha khôngtồntại",null,0)).isEmpty();
        assertThat(searchService.search("%_",null,0)).isEmpty();
        assertThat(searchService.search("DEMO-MC-002",null,0).getContent()).extracting(Product::getSku).containsExactly("DEMO-MC-002");
        assertThat(searchService.search("máy pha cầm tay",null,0).getTotalElements()).isEqualTo(1);
    }
    @Test void normalizedSearchPaginatesWithoutDuplicatesAndHandlesLargePage() throws Exception {
        importer.refresh();
        var first=searchService.search("cà phê",null,0);
        var second=searchService.search("ca phe",null,1);
        assertThat(first.getContent()).hasSize(12);
        assertThat(second.getContent()).isNotEmpty();
        assertThat(first.getContent()).extracting(Product::getId).doesNotContainAnyElementsOf(second.getContent().stream().map(Product::getId).toList());
        assertThat(searchService.search("ca phe",null,Integer.MAX_VALUE).getContent()).isEmpty();
        assertThat(searchService.search("ca phe",null,Integer.MAX_VALUE).getTotalElements()).isEqualTo(first.getTotalElements());
    }
    @Test void relatedProductsAreSameCategoryVisibleAndRankedByAvailabilityThenPrice() throws Exception {
        importer.refresh();
        var current=products.findBySku("DEMO-CF-004").orElseThrow();
        var hidden=products.findBySku("DEMO-CF-005").orElseThrow();hidden.setActive(false);
        var out=products.findBySku("DEMO-CF-006").orElseThrow();out.setStock(0);out.setPrice(current.getPrice());
        products.saveAndFlush(hidden);products.saveAndFlush(out);
        var expected=products.findAll().stream()
            .filter(p->p.isActive() && !p.getId().equals(current.getId()) && p.getCategory().getId().equals(current.getCategory().getId()))
            .sorted(java.util.Comparator.comparingInt((Product p)->p.getStock()>0?0:1)
                .thenComparing(p->p.getPrice().subtract(current.getPrice()).abs()).thenComparing(Product::getId))
            .limit(4).map(Product::getId).toList();
        var actual=products.related(current.getId(),current.getCategory().getId(),current.getPrice(),org.springframework.data.domain.PageRequest.of(0,4));
        assertThat(actual).extracting(Product::getId).containsExactlyElementsOf(expected);
        var result=mvc.perform(get("/products/"+current.getId())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm liên quan"))).andReturn();
        var related=(java.util.List<Product>)result.getModelAndView().getModel().get("relatedProducts");
        assertThat(related).extracting(Product::getId).containsExactlyElementsOf(expected);
        current.setHasImage(true);
        assertThat(current.getImageUrl()).isEqualTo("/product-images/"+current.getId());
    }
    @Test void hidesRelatedSectionWhenCategoryHasNoOtherVisibleProducts() throws Exception {
        importer.refresh();
        var current=products.findBySku("DEMO-MC-001").orElseThrow();
        for(var p:products.findAll()) if(!p.getId().equals(current.getId()) && p.getCategory().getId().equals(current.getCategory().getId())) {
            p.setActive(false);products.save(p);
        }
        products.flush();
        mvc.perform(get("/products/"+current.getId())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"related-title\""))));
    }
    @Test void freshImportCreatesAllProductsWithDescriptionsImagesAndOpeningLedger() throws Exception {
        assertThat(importer.refresh()).isEqualTo(new SampleCatalogService.ImportResult(36,0));
        assertThat(categories.count()).isEqualTo(4);
        for (var p:products.findAll()) {
            assertThat(p.getDescription()).contains("Quy cách", "Gợi ý sử dụng", "Bảo quản");
            assertThat(p.getDescription().length()).isLessThanOrEqualTo(2000);
            assertThat(new ClassPathResource("static"+p.getImageUrl()).exists()).isTrue();
            assertThat(p.getImageUrl()).doesNotContain("sample-", "placeholder");
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
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Bảo quản:")));
    }
}
