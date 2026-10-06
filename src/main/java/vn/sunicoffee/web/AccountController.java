package vn.sunicoffee.web;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.*;
import java.security.Principal;

@Controller
@RequestMapping("/account")
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) {this.accounts=accounts;}
    @GetMapping String profile(Principal p,Model m) {
        var u=accounts.byEmail(p.getName());var f=new ProfileForm();
        f.setFullName(u.getFullName());f.setPhone(u.getPhone());f.setAddress(u.getAddress());
        m.addAttribute("form",f);m.addAttribute("email",u.getEmail());return "account/profile";
    }
    @PostMapping("/profile") String save(@Valid @ModelAttribute("form") ProfileForm f,BindingResult result,Principal p,Model m,RedirectAttributes flash) {
        if(result.hasErrors()) {m.addAttribute("email",p.getName());return "account/profile";}
        accounts.profile(p.getName(),f);flash.addFlashAttribute("success","Đã cập nhật hồ sơ.");return "redirect:/account";
    }
    @GetMapping("/password") String password(Model m) {m.addAttribute("form",new PasswordForm());return "account/password";}
    @PostMapping("/password") String change(@Valid @ModelAttribute("form") PasswordForm f,BindingResult result,Authentication auth,HttpServletRequest request,HttpServletResponse response) {
        if(!result.hasErrors()) {
            try {
                accounts.password(auth.getName(),f);
                new SecurityContextLogoutHandler().logout(request,response,auth);
                return "redirect:/login?passwordChanged";
            } catch(BusinessException e) {result.reject("password",e.getMessage());}
        }
        return "account/password";
    }
}
