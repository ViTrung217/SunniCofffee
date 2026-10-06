package vn.sunicoffee.web.form;
import jakarta.validation.constraints.NotNull;
import vn.sunicoffee.domain.Role;
public class AdminUserCreateForm extends RegistrationForm {
 @NotNull private Role role=Role.CUSTOMER;
 public Role getRole() {return role;}
 public void setRole(Role role) {this.role=role;}
}
