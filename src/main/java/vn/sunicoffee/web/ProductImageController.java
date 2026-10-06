package vn.sunicoffee.web;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import vn.sunicoffee.service.*;

@Controller
public class ProductImageController {
    private final ProductImageService images;
    public ProductImageController(ProductImageService images) {this.images=images;}
    @GetMapping("/product-images/{id}") @ResponseBody ResponseEntity<byte[]> get(@PathVariable Long id,Authentication auth) {
        boolean admin=auth!=null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        var image=images.get(id,admin);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mediaType())).cacheControl(CacheControl.noCache()).body(image.content());
    }
    @PostMapping("/admin/products/{id}/image") String upload(@PathVariable Long id,@RequestParam("image") MultipartFile image,RedirectAttributes flash) {
        try {images.upload(id,image);flash.addFlashAttribute("success","Đã cập nhật ảnh sản phẩm.");}
        catch(BusinessException e) {flash.addFlashAttribute("error",e.getMessage());}
        return "redirect:/admin/products/"+id+"/edit";
    }
    @PostMapping("/admin/products/{id}/image/remove") String remove(@PathVariable Long id,RedirectAttributes flash) {
        images.remove(id);flash.addFlashAttribute("success","Đã gỡ ảnh sản phẩm.");return "redirect:/admin/products/"+id+"/edit";
    }
}
