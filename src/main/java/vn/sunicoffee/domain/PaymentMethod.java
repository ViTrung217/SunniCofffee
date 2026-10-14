package vn.sunicoffee.domain;
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"), SEPAY("Chuyển khoản QR qua SePay");
    private final String label;
    PaymentMethod(String label) { this.label=label; }
    public String getLabel() { return label; }
}
