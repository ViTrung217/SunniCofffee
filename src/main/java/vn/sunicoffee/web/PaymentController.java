package vn.sunicoffee.web;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.sunicoffee.payment.*;
import vn.sunicoffee.service.BusinessException;

@Controller
public class PaymentController {
    private final PaymentStore store; private final PaymentService payments;
    public PaymentController(PaymentStore store,PaymentService payments) {this.store=store;this.payments=payments;}
    @GetMapping("/orders/{id}/payment") String page(@PathVariable long id,Principal user,Model model) {return page(id,user.getName(),false,model);}
    @GetMapping("/admin/orders/{id}/payment") String adminPage(@PathVariable long id,Principal user,Model model) {return page(id,user.getName(),true,model);}
    private String page(long id,String email,boolean admin,Model model) {
        var payment=store.view(id,email,admin);
        if(!admin && "PAID".equals(payment.status())) return "redirect:/orders/"+id+"/payment/success";
        model.addAttribute("payment",payment);model.addAttribute("admin",admin);return "orders/payment";
    }
    @GetMapping("/orders/{id}/payment/success") String success(@PathVariable long id,Principal user,Model model) {
        var payment=store.view(id,user.getName(),false);
        if(!"PAID".equals(payment.status())) return "redirect:/orders/"+id+"/payment";
        model.addAttribute("payment",payment);
        return "orders/payment-success";
    }
    @PostMapping("/orders/{id}/payment/check") @ResponseBody ResponseEntity<PaymentStore.View> check(@PathVariable long id,Principal user) {return check(id,user.getName(),false);}
    @PostMapping("/admin/orders/{id}/payment/check") @ResponseBody ResponseEntity<PaymentStore.View> adminCheck(@PathVariable long id,Principal user) {return check(id,user.getName(),true);}
    private ResponseEntity<PaymentStore.View> check(long id,String email,boolean admin) {return ResponseEntity.ok().header("Cache-Control","no-store").body(payments.check(id,email,admin));}
    @ExceptionHandler(SepayClient.Unavailable.class) @ResponseBody ResponseEntity<?> unavailable() {
        return ResponseEntity.status(503).header("Cache-Control","no-store").body(Map.of("message","Chưa thể kiểm tra SePay. Không chuyển lại tiền; vui lòng thử kiểm tra sau."));
    }
    @ExceptionHandler(BusinessException.class) @ResponseBody ResponseEntity<?> invalid(BusinessException ex) {
        return ResponseEntity.badRequest().header("Cache-Control","no-store").body(Map.of("message",ex.getMessage()));
    }
}
