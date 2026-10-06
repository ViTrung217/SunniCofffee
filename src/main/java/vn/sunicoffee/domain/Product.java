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

    public String getImageUrl() { return hasImage ? "/product-images/"+id : "/images/product-placeholder.svg"; }
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
