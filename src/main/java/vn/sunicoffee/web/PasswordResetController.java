package vn.sunicoffee.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.sunicoffee.service.*;

@Controller
public class PasswordResetController {
    private final PasswordResetService resets;
    public PasswordResetController(PasswordResetService resets) { this.resets=resets; }
    @ModelAttribute void headers(HttpServletResponse response,Model model) {
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");
        model.addAttribute("resetAvailable",resets.available());
    }
    @GetMapping("/forgot-password") String forgot() {return "auth/forgot-password";}
    @PostMapping("/forgot-password") String request(@RequestParam String email,Model model) {
        try {resets.request(email);} catch(MailException ex) { /* Do not reveal whether an email exists or log a reset URL. */ }
        model.addAttribute("sent",true);return "auth/forgot-password";
    }
    @GetMapping("/reset-password") String form(@RequestParam(defaultValue="") String token,Model model) {
        model.addAttribute("token",token);return "auth/reset-password";
    }
    @PostMapping("/reset-password") String reset(@RequestParam String token,@RequestParam String password,@RequestParam String confirmPassword,Model model) {
        try {resets.reset(token,password,confirmPassword);return "redirect:/login?passwordChanged";}
        catch(BusinessException ex) {model.addAttribute("error",ex.getMessage());model.addAttribute("token",token);return "auth/reset-password";}
    }
}
