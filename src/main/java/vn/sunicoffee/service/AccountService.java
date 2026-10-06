package vn.sunicoffee.service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.UserRepository;
import vn.sunicoffee.web.form.*;

@Service
@Transactional(readOnly=true)
public class AccountService {
    private final UserRepository users;
    private final UserService registration;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    public AccountService(UserRepository u,UserService r,PasswordEncoder e,JdbcTemplate j) { users=u;registration=r;encoder=e;jdbc=j; }
    public AppUser byEmail(String email) { return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    public AppUser byId(Long id) { return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    @Transactional public void profile(String email,ProfileForm f) {
        var user=users.lockByEmail(email).orElseThrow();
        user.setFullName(f.getFullName().strip()); user.setPhone(f.getPhone().strip()); user.setAddress(f.getAddress().strip());
    }
    @Transactional public void password(String email,PasswordForm f) {
        var user=users.lockByEmail(email).orElseThrow();
        if(!encoder.matches(f.getCurrentPassword(),user.getPasswordHash())) throw new BusinessException("Mật khẩu hiện tại không đúng.");
        if(!f.getPassword().equals(f.getConfirmPassword())) throw new BusinessException("Hai mật khẩu mới chưa khớp.");
        if(f.getPassword().length()<8 || f.getPassword().length()>64 || f.getPassword().getBytes(StandardCharsets.UTF_8).length>72)
            throw new BusinessException("Mật khẩu mới cần 8–64 ký tự và không vượt 72 byte UTF-8.");
        if(encoder.matches(f.getPassword(),user.getPasswordHash())) throw new BusinessException("Mật khẩu mới cần khác mật khẩu hiện tại.");
        user.setPasswordHash(encoder.encode(f.getPassword())); user.setAuthVersion(user.getAuthVersion()+1);
    }
    private void adminGuard(String actor) {
        // One database lock serializes role/status changes, including across application instances.
        jdbc.queryForObject("SELECT id FROM account_admin_guard WHERE id=1 FOR UPDATE",Integer.class);
        var user=users.lockByEmail(actor).orElseThrow();
        if(!user.isEnabled() || user.getRole()!=Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    @Transactional public void updateUser(String actor,Long id,AdminUserForm f) {
        adminGuard(actor);
        var target=users.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!Objects.equals(target.getVersion(),f.getVersion())) throw new BusinessException("Tài khoản đã thay đổi. Hãy tải lại trang trước khi lưu.");
        if(target.getEmail().equals(actor) && (!f.isEnabled() || f.getRole()!=Role.ADMIN))
            throw new BusinessException("Bạn không thể tự khóa hoặc hạ quyền quản trị của mình.");
        if(target.isEnabled() && target.getRole()==Role.ADMIN && (!f.isEnabled() || f.getRole()!=Role.ADMIN)
                && users.countByRoleAndEnabledTrue(Role.ADMIN)<=1)
            throw new BusinessException("Cần giữ ít nhất một quản trị viên đang hoạt động.");
        if(target.isEnabled()!=f.isEnabled() || target.getRole()!=f.getRole()) target.setAuthVersion(target.getAuthVersion()+1);
        target.setFullName(f.getFullName().strip()); target.setRole(f.getRole()); target.setEnabled(f.isEnabled());
    }
    @Transactional public void createUser(String actor,AdminUserCreateForm f) {
        adminGuard(actor);
        if(!f.getPassword().equals(f.getConfirmPassword())) throw new BusinessException("Hai mật khẩu chưa khớp.");
        registration.create(f.getFullName(),f.getEmail(),f.getPassword(),f.getRole());
    }
}
