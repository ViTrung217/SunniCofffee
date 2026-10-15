package vn.sunicoffee.web;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sunicoffee.service.*;
@Controller
public class ProductEngagementController {
    private final ProductEngagementService service;
    public ProductEngagementController(ProductEngagementService s){service=s;}
    @GetMapping("/favorites") String favorites(Principal p,@RequestParam(defaultValue="0") int page,Model m){m.addAttribute("products",service.favorites(p.getName(),page));return "store/favorites";}
    @PostMapping("/favorites/{id}/remove") String removeFavorite(@PathVariable long id,Principal p){service.favorite(p.getName(),id,false);return "redirect:/favorites";}
    @PostMapping("/products/{id}/favorite") String favorite(@PathVariable long id,@RequestParam boolean selected,Principal p,RedirectAttributes f) {
        if(p==null)return "redirect:/login";
        service.favorite(p.getName(),id,selected);f.addFlashAttribute("success",selected?"Đã thêm vào yêu thích.":"Đã bỏ yêu thích.");return "redirect:/products/"+id;
    }
    @PostMapping("/products/{id}/reviews") String review(@PathVariable long id,@RequestParam int rating,@RequestParam String comment,Principal p,RedirectAttributes f) {
        if(p==null)return "redirect:/login";
        try{service.review(p.getName(),id,rating,comment);f.addFlashAttribute("success","Đã lưu đánh giá của bạn.");}catch(BusinessException e){f.addFlashAttribute("error",e.getMessage());}
        return "redirect:/products/"+id+"#reviews";
    }
    @PostMapping("/products/{id}/reviews/delete") String delete(@PathVariable long id,Principal p,RedirectAttributes f){
        if(p==null)return "redirect:/login";
        service.deleteReview(p.getName(),id);f.addFlashAttribute("success","Đã xóa đánh giá của bạn.");return "redirect:/products/"+id+"#reviews";
    }
}
