package vn.sunicoffee.web.form;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class RegistrationForm {
    @NotBlank @Size(max=100)
    private String fullName = "";
    @NotBlank @Email @Size(max=190)
    private String email = "";
    @NotBlank @Size(min=8,max=64)
    private String password = "";
    @NotBlank
    private String confirmPassword = "";
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
