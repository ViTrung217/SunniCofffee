package vn.sunicoffee.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sunicoffee.service.*;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final CartService cart;
    public CartController(CartService cart) { this.cart=cart; }
    @GetMapping String view(Principal p,Model m) { var items=cart.items(p.getName()); m.addAttribute("items",items); m.addAttribute("total",cart.total(items)); return "store/cart"; }
    @PostMapping("/add") String add(Principal p,@RequestParam Long productId,@RequestParam(defaultValue="1") int quantity,RedirectAttributes flash) {
        try { cart.add(p.getName(),productId,quantity); flash.addFlashAttribute("success","Đã thêm sản phẩm vào giỏ."); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/cart";
    }
    @PostMapping("/update") String update(Principal p,@RequestParam Long productId,@RequestParam int quantity,RedirectAttributes flash) {
        try { cart.update(p.getName(),productId,quantity); flash.addFlashAttribute("success","Đã cập nhật giỏ hàng."); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/cart";
    }
    @PostMapping("/remove") String remove(Principal p,@RequestParam Long productId) { cart.remove(p.getName(),productId); return "redirect:/cart"; }
}
