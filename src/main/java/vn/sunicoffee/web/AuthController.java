package vn.sunicoffee.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.RegistrationForm;

@Controller
public class AuthController {
    @org.springframework.beans.factory.annotation.Autowired private GoogleAccountService google;
    @org.springframework.beans.factory.annotation.Value("${app.google.enabled:false}") private boolean googleEnabled;
    private final UserService users;
    @ModelAttribute("googleEnabled") boolean googleEnabled() { return googleEnabled; }
    @PostMapping("/login/google/link") String linkGoogle(@RequestParam String password,jakarta.servlet.http.HttpSession session,org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        var pending=(GoogleAccountService.Pending)session.getAttribute(GoogleAccountService.PENDING);
        try {
            if(!googleEnabled) throw new BusinessException("Đăng nhập Google chưa được bật.");
            google.link(pending,password);
            session.removeAttribute(GoogleAccountService.PENDING);
            return "redirect:/oauth2/authorization/google";
        } catch(BusinessException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("googleLinkError",ex instanceof BusinessException?ex.getMessage():"Không thể liên kết. Hãy thử đăng nhập lại.");
            return "redirect:/login?googleError";
        }
    }
    public AuthController(UserService users) { this.users=users; }
    @GetMapping("/login") String login() { return "auth/login"; }
    @GetMapping("/register") String registration(Model m) { m.addAttribute("form",new RegistrationForm()); return "auth/register"; }
    @PostMapping("/register") String register(@Valid @ModelAttribute("form") RegistrationForm f,BindingResult result) {
        if(result.hasErrors()) return "auth/register";
        try { users.register(f); }
        catch(BusinessException e) { result.reject("registration",e.getMessage()); return "auth/register"; }
        catch(DataIntegrityViolationException e) { result.reject("registration","Email đã được sử dụng."); return "auth/register"; }
        return "redirect:/login?registered";
    }
}
