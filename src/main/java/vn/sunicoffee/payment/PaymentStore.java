package vn.sunicoffee.payment;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.OrderRepository;
import vn.sunicoffee.service.*;

@Service
@Transactional(readOnly=true)
public class PaymentStore {
    private final OrderRepository orders; private final JdbcTemplate jdbc; private final OrderHistoryService history;
    private final SepaySettings settings;
    public PaymentStore(OrderRepository orders,JdbcTemplate jdbc,OrderHistoryService history,SepaySettings settings) {
        this.orders=orders;this.jdbc=jdbc;this.history=history;this.settings=settings;
    }
    public record Receipt(BigDecimal amount,String reference,LocalDateTime at) {}
    public record View(long orderId,String method,String status,String label,BigDecimal total,BigDecimal received,
            String code,String description,String bank,String account,String holder,boolean sandbox,boolean payable,
            String qrUrl,LocalDateTime checkedAt,List<Receipt> receipts) {}
    private static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")); }
    private CustomerOrder authorized(long id,String email,boolean admin,boolean lock) {
        var order=(lock?orders.lockById(id):orders.findById(id)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!admin && !order.getUser().getEmail().equals(email)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(order.getPaymentMethod()!=PaymentMethod.SEPAY) throw new BusinessException("Đơn này sử dụng COD.");
        return order;
    }
    public View view(long id,String email,boolean admin) {
        var o=authorized(id,email,admin,false);
        String description=(o.getSepayPrefix()+" "+o.getPaymentCode()).strip();
        boolean payable=o.getStatus()==OrderStatus.PENDING && o.getPaymentStatus()==PaymentStatus.UNPAID
            && settings.ready() && o.isPaymentSandbox()==settings.sandbox();
        String qr=payable && !o.isPaymentSandbox()?UriComponentsBuilder.fromUriString("https://vietqr.app/img")
            .queryParam("bank",o.getSepayBank()).queryParam("acc",o.getSepayQrAccount()).queryParam("amount",o.getTotal().toPlainString())
            .queryParam("des",description).queryParam("template","compact").build().encode().toUriString():null;
        var receipts=jdbc.query("SELECT amount,reference_number,transaction_at FROM payment_receipts WHERE order_id=? ORDER BY transaction_at,provider_transaction_id",
            (r,n)->new Receipt(r.getBigDecimal(1),r.getString(2),r.getObject(3,LocalDateTime.class)),id);
        return new View(id,o.getPaymentMethod().name(),o.getPaymentStatus().name(),o.getPaymentStatus().getLabel(),o.getTotal(),o.getReceivedAmount(),
            o.getPaymentCode(),description,o.getSepayBank(),o.getSepayQrAccount(),o.getSepayHolder(),o.isPaymentSandbox(),payable,qr,o.getLastPaymentCheck(),receipts);
    }
    @Transactional
    public SepayClient.Query prepareCheck(long id,String email,boolean admin) {
        var o=authorized(id,email,admin,true);
        if(!settings.ready() || o.isPaymentSandbox()!=settings.sandbox()) throw new BusinessException("Kênh SePay của đơn chưa sẵn sàng. Vui lòng liên hệ cửa hàng.");
        if(o.getLastPaymentCheck()!=null && o.getLastPaymentCheck().isAfter(now().minusSeconds(15))) return null;
        o.setLastPaymentCheck(now());
        return new SepayClient.Query(o.getPaymentCode(),o.getSepayBankAccountId(),o.isPaymentSandbox());
    }
    @Transactional
    public void apply(long id,List<SepayClient.Transfer> transfers) {
        var o=orders.lockById(id).orElseThrow();
        if(o.getPaymentMethod()!=PaymentMethod.SEPAY) throw new BusinessException("Đơn không nhận thanh toán SePay.");
        boolean changed=false;
        for(var t:transfers) {
            if(!matches(o,t)) continue;
            var prior=jdbc.queryForList("SELECT order_id,amount FROM payment_receipts WHERE provider_transaction_id=?",t.id());
            if(!prior.isEmpty()) {
                if(((Number)prior.getFirst().get("order_id")).longValue()!=id || ((BigDecimal)prior.getFirst().get("amount")).compareTo(t.amountIn())!=0)
                    throw new BusinessException("Giao dịch cần được cửa hàng đối chiếu.");
                continue;
            }
            jdbc.update("INSERT INTO payment_receipts(provider_transaction_id,order_id,amount,reference_number,transaction_at,recorded_at) VALUES (?,?,?,?,?,?)",
                t.id(),id,t.amountIn(),t.reference(),t.at(),now());
            changed=true;
        }
        if(!changed) return;
        BigDecimal received=jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM payment_receipts WHERE order_id=?",BigDecimal.class,id);
        if(received.precision()>18) throw new BusinessException("Số tiền vượt giới hạn đối chiếu.");
        o.setReceivedAmount(received);
        boolean closed=o.getStatus()==OrderStatus.CANCELLED || o.getStatus()==OrderStatus.RETURNED;
        if(closed) o.setPaymentStatus(PaymentStatus.REFUND_REQUIRED);
        else if(received.compareTo(o.getTotal())==0) { o.setPaymentStatus(PaymentStatus.PAID);if(o.getPaidAt()==null) o.setPaidAt(now()); }
        else o.setPaymentStatus(PaymentStatus.REVIEW);
        history.record(o,"SePay",o.getPaymentStatus().getLabel()+" — đã ghi nhận "+received.toPlainString()+" VND"+(o.isPaymentSandbox()?" (Sandbox)":""));
    }
    private boolean matches(CustomerOrder o,SepayClient.Transfer t) {
        if(t.id()==null || !t.id().matches("[a-fA-F0-9-]{36}") || !"in".equals(t.type())
            || !Objects.equals(t.bankAccountId(),o.getSepayBankAccountId()) || !Objects.equals(t.accountNumber(),o.getSepayAccountNumber())
            || (!o.getSepayVa().isBlank() && !Objects.equals(t.va(),o.getSepayVa()))
            || t.amountIn()==null || t.amountOut()==null || t.amountIn().signum()<=0 || t.amountOut().signum()!=0
            || t.amountIn().stripTrailingZeros().scale()>0 || t.amountIn().compareTo(new BigDecimal("999999999999999999"))>0
            || t.at()==null || t.at().isBefore(o.getCreatedAt().withNano(0)) || t.at().isAfter(now().plusMinutes(1))
            || t.reference()==null || t.reference().isBlank() || t.reference().length()>160 || t.content()==null) return false;
        var matcher=Pattern.compile("(?i)(?<![A-Z0-9])SC[A-F0-9]{20}(?![A-Z0-9])").matcher(t.content());
        return matcher.find() && matcher.group().equalsIgnoreCase(o.getPaymentCode()) && !matcher.find();
    }
}
