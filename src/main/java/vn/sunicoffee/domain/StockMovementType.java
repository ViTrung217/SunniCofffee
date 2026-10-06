package vn.sunicoffee.domain;
public enum StockMovementType {
    OPENING("Tồn đầu kỳ"), RECEIPT("Nhập kho"), ISSUE("Xuất kho khác"), SALE("Xuất theo đơn"), CANCEL_RETURN("Hoàn do hủy đơn"), DELIVERY_RETURN("Nhận hàng hoàn");
    private final String label;
    StockMovementType(String label) { this.label=label; }
    public String getLabel() { return label; }
}
