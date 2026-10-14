package vn.sunicoffee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;

/** Explicit, repeatable import of authored sample data. Never resets existing stock or prices. */
@Service
public class SampleCatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryService inventory;
    private final ObjectMapper mapper;
    public SampleCatalogService(ProductRepository products, CategoryRepository categories, InventoryService inventory, ObjectMapper mapper) {
        this.products=products; this.categories=categories; this.inventory=inventory; this.mapper=mapper;
    }
    public record ImportResult(int added, int descriptionsUpdated) {}
    @Transactional(rollbackFor=Exception.class)
    public ImportResult refresh() throws java.io.IOException {
        com.fasterxml.jackson.databind.JsonNode data;
        try (var in=new ClassPathResource("seed/products-demo.json").getInputStream()) { data=mapper.readTree(in); }
        Set<String> codes=new HashSet<>(), skus=new HashSet<>();
        if (!"synthetic_demo".equals(data.path("dataset_type").asText())) throw new IllegalArgumentException("Expected sample catalogue");
        for (var c:data.path("categories")) {
            if(c.path("code").asText().isBlank() || !codes.add(c.path("code").asText()) || c.path("name").asText().isBlank())
                throw new IllegalArgumentException("Invalid sample category");
        }
        for (var item:data.path("products")) {
            String sku=item.path("sku").asText(), description=item.path("description").asText();
            if (!sku.startsWith("DEMO-") || sku.length()>50 || !skus.add(sku) || !codes.contains(item.path("category_code").asText())
                || item.path("name").asText().isBlank() || item.path("name").asText().length()>160
                || description.isBlank() || description.length()>2000 || item.path("source_ref").asText().isBlank()
                || !item.path("price_vnd").isIntegralNumber() || item.path("price_vnd").asLong(-1)<0
                || !item.path("opening_stock").canConvertToInt() || item.path("opening_stock").asInt(-1)<0
                || item.path("sales_unit").asText().isBlank() || item.path("sales_unit").asText().length()>30)
                throw new IllegalArgumentException("Invalid sample product");
        }
        for (var item:data.path("categories")) {
            String code=item.path("code").asText();
            if (categories.findByCode(code).isEmpty()) {
                var c=new Category(); c.setCode(code); c.setName(item.path("name").asText());
                c.setDescription("Danh mục dữ liệu phục vụ đồ án, chưa được cửa hàng xác nhận."); categories.save(c);
            }
        }
        int added=0, updated=0;
        for (var item:data.path("products")) {
            String sku=item.path("sku").asText(); var existing=products.findBySku(sku);
            if (existing.isPresent()) {
                var p=products.lockById(existing.get().getId()).orElseThrow();
                String description=item.path("description").asText();
                if (!description.equals(p.getDescription())) { p.setDescription(description); updated++; }
                continue;
            }
            var p=new Product(); p.setSku(sku); p.setName(item.path("name").asText());
            p.setDescription(item.path("description").asText()); p.setPrice(item.path("price_vnd").decimalValue());
            p.setStock(item.path("opening_stock").asInt()); p.setSalesUnit(item.path("sales_unit").asText()); p.setActive(true);
            p.setCategory(categories.findByCode(item.path("category_code").asText()).orElseThrow()); products.save(p);
            inventory.record(p,null,null,StockMovementType.OPENING,p.getStock(),"Tồn đầu danh mục đồ án "+data.path("dataset_version").asText(),null);
            added++;
        }
        return new ImportResult(added,updated);
    }
}
