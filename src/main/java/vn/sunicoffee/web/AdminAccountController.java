package vn.sunicoffee.web;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import vn.sunicoffee.repository.UserRepository;
import vn.sunicoffee.service.*;
import vn.sunicoffee.web.form.*;

@Controller
@RequestMapping("/admin/users")
public class AdminAccountController {
    private final UserRepository users;
    private final AccountService accounts;
    public AdminAccountController(UserRepository u,AccountService a) {users=u;accounts=a;}
    @GetMapping String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model m) {
        q=q.strip();if(q.length()>100) q=q.substring(0,100);
        m.addAttribute("users",users.search(q,PageRequest.of(Math.max(0,page),20,Sort.by("id"))));m.addAttribute("q",q);return "admin/users";
    }
    @GetMapping("/new") String create(Model m) {m.addAttribute("form",new AdminUserCreateForm());return "admin/user-create";}
    @PostMapping("/new") String create(@Valid @ModelAttribute("form") AdminUserCreateForm f,BindingResult result,Principal p,RedirectAttributes flash) {
        if(!result.hasErrors()) {
            try {accounts.createUser(p.getName(),f);flash.addFlashAttribute("success","Đã tạo tài khoản.");return "redirect:/admin/users";}
            catch(BusinessException e) {result.reject("account",e.getMessage());}
            catch(DataIntegrityViolationException e) {result.reject("account","Email đã tồn tại.");}
        }
        return "admin/user-create";
    }
    @GetMapping("/{id}/edit") String edit(@PathVariable Long id,Model m) {
        var u=accounts.byId(id);var f=new AdminUserForm();f.setFullName(u.getFullName());f.setEnabled(u.isEnabled());f.setRole(u.getRole());f.setVersion(u.getVersion());
        m.addAttribute("form",f);m.addAttribute("account",u);return "admin/user-edit";
    }
    @PostMapping("/{id}/edit") String save(@PathVariable Long id,@Valid @ModelAttribute("form") AdminUserForm f,BindingResult result,Principal p,Model m,RedirectAttributes flash) {
        if(!result.hasErrors()) {
            try {accounts.updateUser(p.getName(),id,f);flash.addFlashAttribute("success","Đã cập nhật tài khoản.");return "redirect:/admin/users";}
            catch(BusinessException e) {result.reject("account",e.getMessage());}
        }
        m.addAttribute("account",accounts.byId(id));return "admin/user-edit";
    }
}
