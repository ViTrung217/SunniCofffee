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

    public String getImageUrl() {
        if (hasImage) return "/product-images/" + id;
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
