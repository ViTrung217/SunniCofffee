package vn.sunicoffee.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final CatalogService catalog;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final OrderService orderService;
    public AdminController(CatalogService s,CategoryRepository c,ProductRepository p,OrderRepository o,OrderService os) {
        catalog=s; categories=c; products=p; orders=o; orderService=os;
    }
    @org.springframework.beans.factory.annotation.Autowired private OrderHistoryService history;
    @org.springframework.beans.factory.annotation.Autowired private ShippingPolicy shipping;
    @GetMapping String home() { return "redirect:/admin/products"; }
    @GetMapping("/products") String products(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model m) {
        if(q.length()>100) q=q.substring(0,100);
        m.addAttribute("products",products.search(q,null,true,PageRequest.of(Math.max(0,page),20,Sort.by("id")))); m.addAttribute("q",q); return "admin/products";
    }
    @org.springframework.beans.factory.annotation.Autowired private ProductImageService imageGallery;
    @GetMapping("/products/new") String newProduct(Model m) { m.addAttribute("form",new ProductForm()); productModel(m,null); return "admin/product-form"; }
    @GetMapping("/products/{id}/edit") String editProduct(@PathVariable Long id,Model m) {
        var p=catalog.product(id); var f=new ProductForm(); f.setSku(p.getSku()); f.setName(p.getName()); f.setDescription(p.getDescription());
        f.setVersion(p.getVersion());
        f.setPrice(p.getPrice()); f.setStock(p.getStock()); f.setSalesUnit(p.getSalesUnit()); f.setCategoryId(p.getCategory().getId()); f.setActive(p.isActive());
        m.addAttribute("gallery",imageGallery.gallery(id)); m.addAttribute("form",f); productModel(m,id); return "admin/product-form";
    }
    @PostMapping("/products/save") String saveProduct(@RequestParam(required=false) Long id,@Valid @ModelAttribute("form") ProductForm form,BindingResult result,Model m,RedirectAttributes flash,java.security.Principal actor) {
        if(!result.hasErrors()) {
            try { catalog.saveProduct(id,form,actor.getName()); flash.addFlashAttribute("success","Đã lưu sản phẩm."); return "redirect:/admin/products"; }
            catch(BusinessException e) { result.reject("product",e.getMessage()); }
            catch(DataIntegrityViolationException e) { result.reject("product","Mã SKU bị trùng hoặc danh mục không hợp lệ."); }
        }
        if(id!=null)m.addAttribute("gallery",imageGallery.gallery(id)); productModel(m,id); return "admin/product-form";
    }
    private void productModel(Model m,Long id) { m.addAttribute("id",id); m.addAttribute("categories",categories.findAllByOrderByNameAsc()); if(id!=null) m.addAttribute("product",catalog.product(id)); }
    @PostMapping("/products/{id}/archive") String archive(@PathVariable Long id,RedirectAttributes flash) {
        catalog.archiveProduct(id); flash.addFlashAttribute("success","Đã ngừng bán sản phẩm. Lịch sử đơn hàng được giữ lại."); return "redirect:/admin/products";
    }
    @GetMapping("/categories") String categories(Model m) { m.addAttribute("categories",categories.findAllByOrderByNameAsc()); return "admin/categories"; }
    @GetMapping("/categories/new") String newCategory(Model m) { m.addAttribute("form",new CategoryForm()); return "admin/category-form"; }
    @GetMapping("/categories/{id}/edit") String editCategory(@PathVariable Long id,Model m) {
        var c=catalog.category(id); var f=new CategoryForm(); f.setCode(c.getCode()); f.setName(c.getName()); f.setDescription(c.getDescription());
        m.addAttribute("form",f); m.addAttribute("id",id); return "admin/category-form";
    }
    @PostMapping("/categories/save") String saveCategory(@RequestParam(required=false) Long id,@Valid @ModelAttribute("form") CategoryForm form,BindingResult result,Model m,RedirectAttributes flash) {
        if(!result.hasErrors()) {
            try { catalog.saveCategory(id,form); flash.addFlashAttribute("success","Đã lưu danh mục."); return "redirect:/admin/categories"; }
            catch(BusinessException e) { result.reject("category",e.getMessage()); }
            catch(DataIntegrityViolationException e) { result.reject("category","Mã hoặc tên danh mục bị trùng."); }
        }
        m.addAttribute("id",id); return "admin/category-form";
    }
    @PostMapping("/categories/{id}/delete") String deleteCategory(@PathVariable Long id,RedirectAttributes flash) {
        try { catalog.deleteCategory(id); flash.addFlashAttribute("success","Đã xóa danh mục trống."); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        catch(DataIntegrityViolationException e) { flash.addFlashAttribute("error","Danh mục đang có sản phẩm nên không thể xóa."); }
        return "redirect:/admin/categories";
    }
    @GetMapping("/orders") String orders(@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="") String q,@RequestParam(required=false) OrderStatus status,
        @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
        @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
        @RequestParam(defaultValue="false") boolean overdue,Model m) {
        q=q.strip(); if(q.length()>100) q=q.substring(0,100);
        m.addAttribute("q",q); m.addAttribute("selectedStatus",status); m.addAttribute("from",from); m.addAttribute("to",to);
        m.addAttribute("overdue",overdue); m.addAttribute("pendingHours",shipping.getPendingHours());
        m.addAttribute("statuses",OrderStatus.values()); m.addAttribute("admin",true);
        if((from!=null && to!=null && from.isAfter(to)) || (to!=null && to.getYear()>9998)) {
            m.addAttribute("error","Khoảng ngày không hợp lệ."); m.addAttribute("orders",Page.empty());
        } else {
            String search=q.toUpperCase(java.util.Locale.ROOT).startsWith("SUNI-") ? q.substring(5) : q;
            m.addAttribute("orders",orders.search(search,status,from==null ? null : from.atStartOfDay(),to==null ? null : to.plusDays(1).atStartOfDay(),overdue ? shipping.pendingCutoff() : null,PageRequest.of(Math.max(0,page),20)));
        }
        return "orders/list";
    }
    @GetMapping("/orders/{id}") String order(@PathVariable Long id,Model m) {
        m.addAttribute("order",orderService.detail(id,null,true)); m.addAttribute("events",history.events(id)); m.addAttribute("admin",true); return "orders/detail";
    }
    @PostMapping("/orders/{id}/status") String status(@PathVariable Long id,@RequestParam OrderStatus status,@RequestParam(defaultValue="") String carrier,@RequestParam(defaultValue="") String trackingCode,@RequestParam(defaultValue="") String reason,RedirectAttributes flash,java.security.Principal actor) {
        try { orderService.transition(id,status,actor.getName(),carrier,trackingCode,reason); flash.addFlashAttribute("success","Đã cập nhật trạng thái đơn."); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/orders/"+id;
    }    @PostMapping("/orders/{id}/return") String receiveReturn(@PathVariable Long id,@RequestParam java.util.Map<String,String> fields,
        java.security.Principal actor,RedirectAttributes flash) {
        try {
            var quantities=new java.util.HashMap<Long,Integer>();
            for(var entry:fields.entrySet()) if(entry.getKey().startsWith("restock_"))
                quantities.put(Long.valueOf(entry.getKey().substring(8)),Integer.valueOf(entry.getValue()));
            orderService.receiveReturn(id,actor.getName(),quantities,fields.get("reason"));
            flash.addFlashAttribute("success","Đã nhận hàng hoàn và cập nhật số lượng còn bán được.");
        } catch(NumberFormatException e) { flash.addFlashAttribute("error","Số lượng hoàn kho không hợp lệ."); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/orders/"+id;
    }
    @PostMapping("/orders/{id}/cod") String settleCod(@PathVariable Long id,java.security.Principal actor,RedirectAttributes flash) {
        try { orderService.settleCod(id,actor.getName()); flash.addFlashAttribute("success","Đã ghi nhận nhận đủ tiền COD."); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/orders/"+id;
    }

}
