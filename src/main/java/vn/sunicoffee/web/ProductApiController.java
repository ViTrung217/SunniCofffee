package vn.sunicoffee.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import vn.sunicoffee.domain.Product;
import vn.sunicoffee.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {
    private final ProductRepository products;
    public ProductApiController(ProductRepository p) { products=p; }
    public record ProductView(Long id,String sku,String name,String description,BigDecimal price,int stock,String salesUnit,String category,String imageUrl) {
        static ProductView from(Product p) { return new ProductView(p.getId(),p.getSku(),p.getName(),p.getDescription(),p.getPrice(),p.getStock(),p.getSalesUnit(),p.getCategory().getName(),p.getImageUrl()); }
    }
    public record ProductPage(List<ProductView> items,int page,int totalPages,long totalElements) {}
    @GetMapping ProductPage list(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long category,@RequestParam(defaultValue="0") int page) {
        if(q.length()>100) q=q.substring(0,100);
        var result=products.search(q,category,false,PageRequest.of(Math.max(0,page),12,Sort.by("id")));
        return new ProductPage(result.getContent().stream().map(ProductView::from).toList(),result.getNumber(),result.getTotalPages(),result.getTotalElements());
    }
    @GetMapping("/{id}") ProductView detail(@PathVariable Long id) {
        return products.findById(id).filter(Product::isActive).map(ProductView::from).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
