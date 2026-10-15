package vn.sunicoffee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.UserService;

@Component
public class DataInitializer implements CommandLineRunner {
    @org.springframework.beans.factory.annotation.Autowired private vn.sunicoffee.service.SampleActivityService activity;
    @Value("${app.seed-activity:false}") private boolean seedActivity;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final UserRepository users;
    private final UserService userService;
    private final Environment environment;
    @Value("${app.seed-demo:false}") private boolean seed;
    @Value("${app.catalog.refresh-sample:false}") private boolean refreshSample;
    private final vn.sunicoffee.service.SampleCatalogService sampleCatalog;
    @Value("${app.bootstrap.email:}") private String adminEmail;
    @Value("${app.bootstrap.password:}") private String adminPassword;
    public DataInitializer(CategoryRepository c,ProductRepository p,UserRepository u,UserService s,Environment e,vn.sunicoffee.service.SampleCatalogService sampleCatalog) {
        categories=c; products=p; users=u; userService=s; environment=e; this.sampleCatalog=sampleCatalog;
    }
    @Override @Transactional
    public void run(String... args) throws Exception {
        if (refreshSample || (seed && categories.count()==0 && products.count()==0)) {
            var result=sampleCatalog.refresh();
            org.slf4j.LoggerFactory.getLogger(DataInitializer.class).info("Sample catalogue: added={}, descriptionsUpdated={}", result.added(), result.descriptionsUpdated());
        }
        if (seedActivity) activity.populate();
        boolean demo=environment.matchesProfiles("demo");
        if (demo) {
            if (!users.existsByEmail("admin@sunicoffee.local")) userService.create("Quản trị demo","admin@sunicoffee.local","SuniDemo!2026",Role.ADMIN);
            if (!users.existsByEmail("khach@sunicoffee.local")) userService.create("Khách demo","khach@sunicoffee.local","SuniDemo!2026",Role.CUSTOMER);
        } else if (!adminEmail.isBlank() && !adminPassword.isBlank() && !users.existsByEmail(adminEmail.strip().toLowerCase(java.util.Locale.ROOT))) {
            userService.create("Quản trị SuniCoffee",adminEmail,adminPassword,Role.ADMIN);
        }
    }
}
