package vn.sunicoffee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="customer_orders")
public class CustomerOrder {
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20)
    private PaymentMethod paymentMethod = PaymentMethod.COD;
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod value) { paymentMethod=value; }
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus value) { paymentStatus=value; }
    @Column(unique=true,length=24)
    private String paymentCode = null;
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String value) { paymentCode=value; }
    @Column(nullable=false,precision=18,scale=0)
    private BigDecimal receivedAmount = BigDecimal.ZERO;
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal value) { receivedAmount=value; }
    @Column
    private LocalDateTime paidAt = null;
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime value) { paidAt=value; }
    @Column
    private LocalDateTime lastPaymentCheck = null;
    public LocalDateTime getLastPaymentCheck() { return lastPaymentCheck; }
    public void setLastPaymentCheck(LocalDateTime value) { lastPaymentCheck=value; }
    @Column(nullable=false)
    private boolean paymentSandbox = false;
    public boolean isPaymentSandbox() { return paymentSandbox; }
    public void setPaymentSandbox(boolean value) { paymentSandbox=value; }
    @Column(length=36)
    private String sepayBankAccountId = null;
    public String getSepayBankAccountId() { return sepayBankAccountId; }
    public void setSepayBankAccountId(String value) { sepayBankAccountId=value; }
    @Column(length=64)
    private String sepayAccountNumber = null;
    public String getSepayAccountNumber() { return sepayAccountNumber; }
    public void setSepayAccountNumber(String value) { sepayAccountNumber=value; }
    @Column(length=64)
    private String sepayQrAccount = null;
    public String getSepayQrAccount() { return sepayQrAccount; }
    public void setSepayQrAccount(String value) { sepayQrAccount=value; }
    @Column(length=40)
    private String sepayBank = null;
    public String getSepayBank() { return sepayBank; }
    public void setSepayBank(String value) { sepayBank=value; }
    @Column(length=100)
    private String sepayHolder = null;
    public String getSepayHolder() { return sepayHolder; }
    public void setSepayHolder(String value) { sepayHolder=value; }
    @Column(length=40)
    private String sepayPrefix = null;
    public String getSepayPrefix() { return sepayPrefix; }
    public void setSepayPrefix(String value) { sepayPrefix=value; }
    @Column(length=64)
    private String sepayVa = null;
    public String getSepayVa() { return sepayVa; }
    public void setSepayVa(String value) { sepayVa=value; }

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
