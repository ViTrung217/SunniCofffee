package vn.sunicoffee.web.form;
import jakarta.validation.constraints.*;
import vn.sunicoffee.domain.*;
public class AdminUserForm {
    @NotBlank @Size(max=100)
    private String fullName = "";
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName=fullName; }
    @NotNull
    private Role role;
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role=role; }

    private boolean enabled;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled=enabled; }
    @NotNull
    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version=version; }
}
