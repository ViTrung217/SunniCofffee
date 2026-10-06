package vn.sunicoffee.web.form;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductForm {
    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version=version; }
    @NotBlank @Pattern(regexp="[A-Z0-9_-]{2,50}", message="SKU gồm 2–50 chữ in hoa, số, dấu gạch")
    private String sku = "";
    @NotBlank @Size(max=160)
    private String name = "";
    @NotBlank @Size(max=2000)
    private String description = "";
    @NotNull @DecimalMin("1") @Digits(integer=12,fraction=0)
    private BigDecimal price;
    @NotNull @Min(0) @Max(2147483647)
    private Integer stock;
    @NotBlank @Size(max=30)
    private String salesUnit = "";
    @NotNull
    private Long categoryId;

    private boolean active = true;
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
