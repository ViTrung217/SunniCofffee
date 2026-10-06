package vn.sunicoffee.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.UserService;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final UserRepository users;
    private final UserService userService;
    private final ObjectMapper mapper;
    private final Environment environment;
    private final vn.sunicoffee.service.InventoryService inventory;
    @Value("${app.seed-demo:false}") private boolean seed;
    @Value("${app.bootstrap.email:}") private String adminEmail;
    @Value("${app.bootstrap.password:}") private String adminPassword;
    public DataInitializer(CategoryRepository c,ProductRepository p,UserRepository u,UserService s,ObjectMapper m,Environment e,vn.sunicoffee.service.InventoryService inventory) {
        categories=c; products=p; users=u; userService=s; mapper=m; environment=e; this.inventory=inventory;
    }
    @Override @Transactional
    public void run(String... args) throws Exception {
        if (seed && categories.count()==0 && products.count()==0) {
            try (var input=new ClassPathResource("seed/products-demo.json").getInputStream()) {
                var data=mapper.readTree(input);
                for (var node:data.get("categories")) {
                    Category c=new Category(); c.setCode(node.get("code").asText()); c.setName(node.get("name").asText());
                    c.setDescription("Danh mục dữ liệu mô phỏng"); categories.save(c);
                }
                for (var node:data.get("products")) {
                    Product p=new Product(); p.setSku(node.get("sku").asText()); p.setName(node.get("name").asText());
                    p.setDescription(node.get("description").asText()); p.setPrice(node.get("price_vnd").decimalValue());
                    p.setStock(node.get("opening_stock").asInt()); p.setSalesUnit(node.get("sales_unit").asText()); p.setActive(true);
                    p.setCategory(categories.findByCode(node.get("category_code").asText()).orElseThrow()); products.save(p);
                    inventory.record(p,null,null,StockMovementType.OPENING,p.getStock(),"Tồn đầu dữ liệu demo",null);
                }
            }
        }
        boolean demo=environment.matchesProfiles("demo");
        if (demo) {
            if (!users.existsByEmail("admin@sunicoffee.local")) userService.create("Quản trị demo","admin@sunicoffee.local","SuniDemo!2026",Role.ADMIN);
            if (!users.existsByEmail("khach@sunicoffee.local")) userService.create("Khách demo","khach@sunicoffee.local","SuniDemo!2026",Role.CUSTOMER);
        } else if (!adminEmail.isBlank() && !adminPassword.isBlank() && !users.existsByEmail(adminEmail.strip().toLowerCase(java.util.Locale.ROOT))) {
            userService.create("Quản trị SuniCoffee",adminEmail,adminPassword,Role.ADMIN);
        }
    }
}
