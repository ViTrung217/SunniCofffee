package vn.sunicoffee.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.sunicoffee.repository.ProductRepository;

@ControllerAdvice
public class ViewAdvice {
    private final Environment environment;
    private final ProductRepository products;
    public ViewAdvice(Environment e, ProductRepository products) { environment=e; this.products=products; }
    @ModelAttribute("demoMode") public boolean demoMode() {
        return environment.matchesProfiles("demo") || products.existsByActiveTrueAndSkuStartingWithIgnoreCase("DEMO-");
    }
}
