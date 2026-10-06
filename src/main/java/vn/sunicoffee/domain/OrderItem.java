package vn.sunicoffee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="order_id")
    private CustomerOrder order;
    @ManyToOne(optional=false) @JoinColumn(name="product_id")
    private Product product;
    @Column(nullable=false, length=160)
    private String productName;
    @Column(nullable=false, length=50)
    private String sku;
    @Column(nullable=false, precision=15, scale=0)
    private BigDecimal unitPrice;
    @Column(nullable=false)
    private int quantity;

    @Column private Integer restockedQuantity;
    public Integer getRestockedQuantity() { return restockedQuantity; }
    public void setRestockedQuantity(Integer value) { restockedQuantity=value; }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
