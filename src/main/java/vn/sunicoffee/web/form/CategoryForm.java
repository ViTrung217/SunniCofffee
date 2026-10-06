package vn.sunicoffee.web.form;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CategoryForm {
    @NotBlank @Pattern(regexp="[A-Z0-9_-]{2,30}", message="Mã gồm 2–30 chữ in hoa, số, dấu gạch")
    private String code = "";
    @NotBlank @Size(max=100)
    private String name = "";
    @Size(max=1000)
    private String description = "";
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
