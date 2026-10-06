package vn.sunicoffee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.UserRepository;
import vn.sunicoffee.web.form.RegistrationForm;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public UserService(UserRepository users,PasswordEncoder encoder) { this.users=users; this.encoder=encoder; }
    @Transactional
    public AppUser register(RegistrationForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) throw new BusinessException("Hai mật khẩu chưa khớp.");
        return create(form.getFullName(),form.getEmail(),form.getPassword(),Role.CUSTOMER);
    }
    @Transactional
    public AppUser create(String name,String email,String password,Role role) {
        email=email.strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new BusinessException("Email đã được sử dụng.");
        if (password.length()<8 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new BusinessException("Mật khẩu cần ít nhất 8 ký tự và không vượt quá 72 byte UTF-8.");
        AppUser u=new AppUser(); u.setFullName(name.strip()); u.setEmail(email);
        u.setPasswordHash(encoder.encode(password)); u.setRole(role); u.setEnabled(true);
        return users.save(u);
    }
}
