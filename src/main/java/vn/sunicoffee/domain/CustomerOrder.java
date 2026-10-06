package vn.sunicoffee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="customer_orders")
public class CustomerOrder {
    @Column
    private LocalDateTime completedAt;
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt=completedAt; }

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="user_id")
    private AppUser user;
    @Column(nullable=false, unique=true, length=36)
    private String checkoutToken;
    @Column(nullable=false, length=100)
    private String recipientName;
    @Column(nullable=false, length=20)
    private String phone;
    @Column(nullable=false, length=500)
    private String address;
    @Column(nullable=false, length=500)
    private String note;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private OrderStatus status;
    @Column(nullable=false, precision=18, scale=0)
    private BigDecimal total;
    @Column(nullable=false)
    private LocalDateTime createdAt;
    @OneToMany(mappedBy="order", cascade=CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable=false, precision=15, scale=0)
    private BigDecimal shippingFee = BigDecimal.ZERO;
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal value) { shippingFee=value; }
    @Column(nullable=false, length=100)
    private String carrier = "";
    public String getCarrier() { return carrier; }
    public void setCarrier(String value) { carrier=value; }
    @Column(nullable=false, length=100)
    private String trackingCode = "";
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String value) { trackingCode=value; }
    @Column
    private LocalDateTime shippedAt = null;
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime value) { shippedAt=value; }
    @Column
    private LocalDateTime returnedAt = null;
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime value) { returnedAt=value; }
    @Column
    private LocalDateTime codSettledAt = null;
    public LocalDateTime getCodSettledAt() { return codSettledAt; }
    public void setCodSettledAt(LocalDateTime value) { codSettledAt=value; }
    public BigDecimal getMerchandiseTotal() { return total.subtract(shippingFee); }
    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String checkoutToken) { this.checkoutToken = checkoutToken; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
