package vn.sunicoffee.domain;
public enum PaymentStatus {
    UNPAID("Chưa thanh toán"), PAID("Đã thanh toán"), REVIEW("Cần đối chiếu số tiền"), REFUND_REQUIRED("Cần xử lý hoàn tiền");
    private final String label;
    PaymentStatus(String label) { this.label=label; }
    public String getLabel() { return label; }
}
