package vn.sunicoffee.web.form;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CheckoutForm {
    @NotNull
    private vn.sunicoffee.domain.PaymentMethod paymentMethod = vn.sunicoffee.domain.PaymentMethod.COD;
    public vn.sunicoffee.domain.PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(vn.sunicoffee.domain.PaymentMethod value) { paymentMethod=value; }

    @NotBlank @Size(max=100)
    private String recipientName = "";
    @NotBlank @Pattern(regexp="[0-9+ ()-]{9,20}", message="Số điện thoại cần 9–20 ký tự hợp lệ")
    private String phone = "";
    @NotBlank @Size(min=10,max=500)
    private String address = "";
    @Size(max=500)
    private String note = "";
    @NotBlank @Pattern(regexp="[0-9a-f-]{36}")
    private String checkoutToken = "";
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String checkoutToken) { this.checkoutToken = checkoutToken; }
}
