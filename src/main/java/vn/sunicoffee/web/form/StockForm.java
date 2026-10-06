package vn.sunicoffee.web.form;
import jakarta.validation.constraints.*;
import vn.sunicoffee.domain.*;
public class StockForm {
    @NotNull
    private Long productId;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId=productId; }
    @NotNull
    private StockMovementType type;
    public StockMovementType getType() { return type; }
    public void setType(StockMovementType type) { this.type=type; }
    @NotNull @Min(1) @Max(1000000)
    private Integer quantity;
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity=quantity; }
    @NotBlank @Size(max=500)
    private String reason = "";
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason=reason; }
    @NotBlank @Pattern(regexp="[0-9a-f-]{36}")
    private String requestToken = "";
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String requestToken) { this.requestToken=requestToken; }
}
