package vn.sunicoffee.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import vn.sunicoffee.repository.OrderRepository;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.CheckoutForm;

@Controller
public class OrderController {
    private final CartService cart;
    private final OrderService orders;
    private final OrderRepository repository;
    private final AccountService accounts;
    public OrderController(CartService c,OrderService o,OrderRepository r,AccountService a) { cart=c; orders=o; repository=r; accounts=a; }
    @org.springframework.beans.factory.annotation.Autowired private OrderHistoryService history;
    @org.springframework.beans.factory.annotation.Autowired private ShippingPolicy shipping;
    @org.springframework.beans.factory.annotation.Autowired private vn.sunicoffee.payment.SepaySettings sepay;
    @GetMapping("/checkout") String checkout(Principal p,HttpSession session,Model m) {
        if(cart.items(p.getName()).isEmpty()) return "redirect:/cart";
        String token=CheckoutTokens.issue(session);
        CheckoutForm form=new CheckoutForm(); form.setCheckoutToken(token);
        var account=accounts.byEmail(p.getName());form.setRecipientName(account.getFullName());form.setPhone(account.getPhone());form.setAddress(account.getAddress()); m.addAttribute("form",form);
        summary(p,m); return "store/checkout";
    }
    @PostMapping("/checkout") String submit(@Valid @ModelAttribute("form") CheckoutForm form,BindingResult result,
                                             Principal p,HttpSession session,Model m) {
        if(!CheckoutTokens.contains(session,form.getCheckoutToken())) result.reject("token","Phiên đặt hàng đã thay đổi. Hãy mở lại trang thanh toán từ giỏ hàng.");
        if(!result.hasErrors()) {
            try { var order=orders.checkout(p.getName(),form); return "redirect:/orders/"+order.getId()+(order.getPaymentMethod()==vn.sunicoffee.domain.PaymentMethod.SEPAY ? "/payment" : ""); }
            catch(BusinessException e) { result.reject("checkout",e.getMessage()); }
            catch(org.springframework.dao.ConcurrencyFailureException e) { result.reject("checkout","Hệ thống đang xử lý tồn kho. Vui lòng thử đặt hàng lại; giỏ hàng của bạn vẫn được giữ."); }
        }
        summary(p,m); return "store/checkout";
    }
    private void summary(Principal p,Model m) { m.addAttribute("sepayReady",sepay.ready()); m.addAttribute("sepaySandbox",sepay.sandbox()); var items=cart.items(p.getName()); m.addAttribute("items",items); m.addAttribute("shippingFee",shipping.getFee()); m.addAttribute("total",cart.total(items).add(shipping.getFee())); }
    @GetMapping("/orders") String history(Principal p,@RequestParam(defaultValue="0") int page,Model m) {
        m.addAttribute("orders",repository.findByUserEmailOrderByCreatedAtDesc(p.getName(),PageRequest.of(Math.max(0,page),20))); m.addAttribute("admin",false); return "orders/list";
    }
    @GetMapping("/orders/{id}") String detail(@PathVariable Long id,Principal p,Model m) {
        m.addAttribute("order",orders.detail(id,p.getName(),false)); m.addAttribute("events",history.events(id)); m.addAttribute("admin",false); return "orders/detail";
    }
    @PostMapping("/orders/{id}/cancel") String cancel(@PathVariable Long id,Principal p,RedirectAttributes flash) {
        try { orders.cancelByCustomer(id,p.getName()); flash.addFlashAttribute("success","Đã hủy đơn và hoàn lại tồn kho."); }
        catch(BusinessException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/orders/"+id;
    }
}
