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
    private final UserService users;
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
