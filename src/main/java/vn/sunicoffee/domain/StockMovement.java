package vn.sunicoffee.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="stock_movements")
public class StockMovement {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    public Long getId() { return id; }
    @ManyToOne(optional=false) @JoinColumn(name="product_id")
    private Product product;
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product=product; }
    @ManyToOne @JoinColumn(name="actor_id")
    private AppUser actor;
    public AppUser getActor() { return actor; }
    public void setActor(AppUser actor) { this.actor=actor; }
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id")
    private CustomerOrder order;
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order=order; }
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20)
    private StockMovementType type;
    public StockMovementType getType() { return type; }
    public void setType(StockMovementType type) { this.type=type; }
    @Column(nullable=false)
    private int quantityDelta;
    public int getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(int quantityDelta) { this.quantityDelta=quantityDelta; }
    @Column(nullable=false)
    private int balanceAfter;
    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter=balanceAfter; }
    @Column(nullable=false,length=500)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason=reason; }
    @Column(unique=true,length=36)
    private String requestToken;
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String requestToken) { this.requestToken=requestToken; }
    @Column(nullable=false)
    private LocalDateTime createdAt;
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt=createdAt; }
}
