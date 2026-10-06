package vn.sunicoffee.domain;
public enum OrderStatus {
    PENDING("Chờ xác nhận"), CONFIRMED("Đã xác nhận"), SHIPPING("Đang giao"), COMPLETED("Hoàn thành"), CANCELLED("Đã hủy"), DELIVERY_FAILED("Giao thất bại"), RETURNING("Đang hoàn hàng"), RETURNED("Đã nhận hàng hoàn");
    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
