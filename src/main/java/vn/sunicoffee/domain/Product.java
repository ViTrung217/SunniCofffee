package vn.sunicoffee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="products")
public class Product {
    @Column(nullable=false)
    private boolean hasImage = false;
    public boolean isHasImage() { return hasImage; }
    public void setHasImage(boolean hasImage) { this.hasImage=hasImage; }

    @Version
    @Column(nullable=false)
    private Long version;
    public Long getVersion() { return version; }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=50)
    private String sku;
    @Column(nullable=false, length=160)
    private String name;
    @Column(nullable=false, length=2000)
    private String description;
    @Column(nullable=false, precision=15, scale=0)
    private BigDecimal price;
    @Column(nullable=false)
    private int stock;
    @Column(nullable=false, length=30)
    private String salesUnit;
    @Column(nullable=false)
    private boolean active;
    @ManyToOne(optional=false) @JoinColumn(name="category_id")
    private Category category;

    private static final java.util.Set<String> DEMO_IMAGE_SKUS = java.util.Set.of(
        "DEMO-CF-001", "DEMO-CF-002", "DEMO-CF-003",
        "DEMO-MC-001", "DEMO-MC-002", "DEMO-MC-003",
        "DEMO-BR-001", "DEMO-BR-002", "DEMO-BR-003",
        "DEMO-OD-001", "DEMO-OD-002", "DEMO-OD-003");

    private static final java.util.Set<String> CATALOG_IMAGE_SKUS = java.util.Set.of(
        "DEMO-CF-004", "DEMO-CF-005", "DEMO-CF-006", "DEMO-CF-007", "DEMO-CF-008", "DEMO-CF-009", "DEMO-CF-010", "DEMO-MC-004", "DEMO-MC-005", "DEMO-MC-006", "DEMO-BR-004", "DEMO-BR-005", "DEMO-BR-006", "DEMO-BR-007", "DEMO-BR-008", "DEMO-BR-009", "DEMO-BR-010", "DEMO-BR-011", "DEMO-BR-012", "DEMO-OD-004", "DEMO-OD-005", "DEMO-OD-006", "DEMO-OD-007", "DEMO-OD-008",
        "DEMO-CF-011", "DEMO-CF-012", "DEMO-CF-013", "DEMO-CF-014", "DEMO-MC-007", "DEMO-MC-008", "DEMO-BR-013", "DEMO-BR-014", "DEMO-BR-015", "DEMO-BR-016", "DEMO-BR-017", "DEMO-OD-009", "DEMO-OD-010", "DEMO-OD-011");

    public String getImageUrl() {
        if (hasImage) return "/product-images/" + id;
        if (sku != null && CATALOG_IMAGE_SKUS.contains(sku)) return "/images/products/" + sku.toLowerCase(java.util.Locale.ROOT) + ".jpg";
        if (sku != null && DEMO_IMAGE_SKUS.contains(sku)) {
            return "/images/products/" + sku.toLowerCase(java.util.Locale.ROOT) + ".png";
        }
        if (sku != null && sku.matches("DEMO-(CF|MC|BR|OD)-[0-9]{3}")) {
            return "/images/products/sample-" + sku.substring(5,7).toLowerCase(java.util.Locale.ROOT) + ".svg";
        }
        return "/images/product-placeholder.svg";
    }
    public Long getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
