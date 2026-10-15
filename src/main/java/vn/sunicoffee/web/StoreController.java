package vn.sunicoffee.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;

@Controller
public class StoreController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CatalogService catalog;
    public StoreController(ProductRepository p,CategoryRepository c,CatalogService s) { products=p; categories=c; catalog=s; }
    @org.springframework.beans.factory.annotation.Autowired private ProductEngagementService engagement;
    @org.springframework.beans.factory.annotation.Autowired private ProductImageService images;
    @GetMapping({"/", "/products"})
    String index(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long category,
                 @RequestParam(defaultValue="0") int page,Model model,jakarta.servlet.http.HttpServletRequest request) {
        String search=q.strip(); if(search.length()>100) search=search.substring(0,100);
        model.addAttribute("home",request.getRequestURI().equals(request.getContextPath() + "/") && search.isEmpty() && category==null && page==0);
        model.addAttribute("products",products.search(search,category,false,PageRequest.of(Math.max(0,page),12,Sort.by("id"))));
        model.addAttribute("categories",categories.findAllByOrderByNameAsc()); model.addAttribute("q",search); model.addAttribute("selectedCategory",category);
        return "store/catalog";
    }
    @GetMapping("/products/{id}")
    String detail(@PathVariable Long id,@RequestParam(defaultValue="0") int reviewPage,java.security.Principal principal,Model model) {
        var p=catalog.product(id); if(!p.isActive()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String email=principal==null?null:principal.getName();
        model.addAttribute("gallery",images.gallery(id));
        model.addAttribute("favorite",engagement.favorite(email,id));
        model.addAttribute("canReview",engagement.eligible(email,id));
        model.addAttribute("ownReview",engagement.own(email,id));
        model.addAttribute("reviews",engagement.reviews(id,reviewPage));
        model.addAttribute("reviewSummary",engagement.summary(id));
        model.addAttribute("reviewPage",Math.max(0,reviewPage));
        model.addAttribute("relatedProducts",products.related(p.getId(),p.getCategory().getId(),p.getPrice(),PageRequest.of(0,4)));
        model.addAttribute("product",p); return "store/product";
    }
}
