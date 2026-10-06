package vn.sunicoffee.service;

import java.math.BigDecimal;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShippingPolicy {
    private final BigDecimal fee;
    private final int pendingHours;
    public ShippingPolicy(@Value("${app.shipping.fee:0}") BigDecimal fee,
                          @Value("${app.shipping.pending-hours:48}") int pendingHours) {
        if(fee.signum()<0 || fee.stripTrailingZeros().scale()>0 || fee.compareTo(new BigDecimal("10000000"))>0 || pendingHours<1)
            throw new IllegalArgumentException("Phí giao hàng hoặc thời hạn nhắc xử lý không hợp lệ");
        this.fee=fee; this.pendingHours=pendingHours;
    }
    public BigDecimal getFee() { return fee; }
    public int getPendingHours() { return pendingHours; }
    public LocalDateTime pendingCutoff() { return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(pendingHours); }
}
