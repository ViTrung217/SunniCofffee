package vn.sunicoffee.web.form;
import jakarta.validation.constraints.*;
import vn.sunicoffee.domain.*;
public class PasswordForm {
    @NotBlank
    private String currentPassword = "";
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword=currentPassword; }
    @NotBlank @Size(min=8,max=64)
    private String password = "";
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password=password; }
    @NotBlank
    private String confirmPassword = "";
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword=confirmPassword; }
}
