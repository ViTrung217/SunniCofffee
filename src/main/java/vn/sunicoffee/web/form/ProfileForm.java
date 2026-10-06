package vn.sunicoffee.web.form;
import jakarta.validation.constraints.*;
import vn.sunicoffee.domain.*;
public class ProfileForm {
    @NotBlank @Size(max=100)
    private String fullName = "";
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName=fullName; }
    @Pattern(regexp="[0-9+ ()-]{0,20}")
    private String phone = "";
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone=phone; }
    @Size(max=500)
    private String address = "";
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address=address; }
}
