package vn.sunicoffee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.web.form.CheckoutForm;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

@Service
@Transactional(readOnly=true)
public class OrderService {
    private final OrderRepository orders;
    private final UserRepository users;
    private final ProductRepository products;
    private final CartRepository carts;
    private final InventoryService inventory;
    public OrderService(OrderRepository o,UserRepository u,ProductRepository p,CartRepository c,InventoryService inventory) { orders=o; users=u; products=p; carts=c; this.inventory=inventory; }
    @org.springframework.beans.factory.annotation.Autowired private OrderHistoryService history;
    @org.springframework.beans.factory.annotation.Autowired private ShippingPolicy shipping;
    @Transactional
    public CustomerOrder checkout(String email,CheckoutForm f) {
        // Serialize cart mutations and checkout for the same account.
        AppUser u=users.lockByEmail(email).orElseThrow();
        var previous=orders.findByCheckoutToken(f.getCheckoutToken());
        if (previous.isPresent()) {
            if (!previous.get().getUser().getId().equals(u.getId())) throw new BusinessException("Phiên đặt hàng không hợp lệ.");
            return previous.get();
        }
        var cart=carts.findByUserIdOrderByProductIdAsc(u.getId());
        if (cart.isEmpty()) throw new BusinessException("Giỏ hàng đang trống.");
        CustomerOrder order=new CustomerOrder(); order.setUser(u); order.setCheckoutToken(f.getCheckoutToken());
        order.setRecipientName(f.getRecipientName().strip()); order.setPhone(f.getPhone().strip());
        order.setAddress(f.getAddress().strip()); order.setNote(f.getNote().strip());
        order.setStatus(OrderStatus.PENDING); order.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        BigDecimal total=BigDecimal.ZERO;
        // Always lock products in ascending ID order to reduce deadlocks.
        for (CartItem line:cart) {
            Product p=line.getProduct();
            // Refresh prevents a previously loaded cart association from providing stale stock.
            refreshProduct(p);
            if (!p.isActive() || p.getStock()<line.getQuantity()) throw new BusinessException("Sản phẩm '"+p.getName()+"' không còn đủ hàng. Hãy sửa giỏ hàng.");
            p.setStock(p.getStock()-line.getQuantity());
            OrderItem item=new OrderItem(); item.setOrder(order); item.setProduct(p); item.setSku(p.getSku());
            item.setProductName(p.getName()); item.setUnitPrice(p.getPrice()); item.setQuantity(line.getQuantity());
            order.getItems().add(item); total=total.add(item.getSubtotal());
        }
        order.setShippingFee(shipping.getFee()); order.setTotal(total.add(order.getShippingFee())); orders.save(order);
        history.record(order,email,"Đặt hàng COD"); carts.deleteAll(cart);
        for(OrderItem item:order.getItems()) inventory.record(item.getProduct(),u,order,StockMovementType.SALE,-item.getQuantity(),"Đặt đơn SUNI-"+order.getId(),null);
        return order;
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private void refreshProduct(Product p) { entityManager.refresh(p,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); }
    public CustomerOrder detail(Long id,String email,boolean admin) {
        CustomerOrder o=orders.detail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!admin && !o.getUser().getEmail().equals(email)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return o;
    }
    @Transactional
    public void cancelByCustomer(Long id,String email) {
        CustomerOrder o=orders.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!o.getUser().getEmail().equals(email)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (o.getStatus()!=OrderStatus.PENDING) throw new BusinessException("Chỉ có thể hủy đơn đang chờ xác nhận.");
        cancel(o,o.getUser());
        history.record(o,email,"Khách hủy trước khi xác nhận");
    }
    @Transactional
    public void transition(Long id,OrderStatus next) {
        transition(id,next,null);
    }
    @Transactional
    public void transition(Long id,OrderStatus next,String actorEmail) {
        transition(id,next,actorEmail,"", "", "");
    }
    @Transactional
    public void transition(Long id,OrderStatus next,String actorEmail,String carrier,String tracking,String reason) {
        CustomerOrder o=orders.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        reason=reason==null ? "" : reason.strip();
        if(reason.length()>500) throw new BusinessException("Lý do tối đa 500 ký tự.");
        boolean valid=switch(o.getStatus()) {
            case PENDING -> next==OrderStatus.CONFIRMED || next==OrderStatus.CANCELLED;
            case CONFIRMED -> next==OrderStatus.SHIPPING || next==OrderStatus.CANCELLED;
            case SHIPPING -> next==OrderStatus.COMPLETED || next==OrderStatus.DELIVERY_FAILED;
            case DELIVERY_FAILED -> next==OrderStatus.SHIPPING || next==OrderStatus.RETURNING;
            default -> false;
        };
        if (!valid) throw new BusinessException("Không thể chuyển trạng thái đơn hàng theo lựa chọn này.");
        if((next==OrderStatus.CANCELLED || next==OrderStatus.DELIVERY_FAILED || next==OrderStatus.RETURNING) && reason.isBlank())
            throw new BusinessException("Vui lòng nhập lý do hủy, giao thất bại hoặc hoàn hàng.");
        if(next==OrderStatus.SHIPPING) {
            carrier=carrier==null ? "" : carrier.strip(); tracking=tracking==null ? "" : tracking.strip();
            if(carrier.isBlank()) carrier=o.getCarrier();
            if(tracking.isBlank()) tracking=o.getTrackingCode();
            if(carrier.isBlank() || tracking.isBlank() || carrier.length()>100 || tracking.length()>100)
                throw new BusinessException("Nhập đơn vị vận chuyển và mã vận đơn (tối đa 100 ký tự).");
            o.setCarrier(carrier); o.setTrackingCode(tracking);
            if(o.getShippedAt()==null) o.setShippedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        }
        if (next==OrderStatus.CANCELLED) cancel(o,actorEmail==null ? null : users.findByEmail(actorEmail).orElseThrow());
        else {
            o.setStatus(next);
            if(next==OrderStatus.COMPLETED) o.setCompletedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        }
        history.record(o,actorEmail,(reason.isBlank() ? next.getLabel() : reason)
            +(next==OrderStatus.SHIPPING ? " — "+o.getCarrier()+" / "+o.getTrackingCode() : ""));
    }
    @Transactional
    public void receiveReturn(Long id,String actorEmail,java.util.Map<Long,Integer> quantities,String reason) {
        CustomerOrder o=orders.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(o.getStatus()!=OrderStatus.RETURNING) throw new BusinessException("Chỉ nhận hoàn khi đơn đang hoàn hàng.");
        if(reason==null || reason.isBlank() || reason.length()>500) throw new BusinessException("Nhập kết quả kiểm tra hàng hoàn (tối đa 500 ký tự).");
        if(quantities==null || quantities.size()!=o.getItems().size()) throw new BusinessException("Cần nhập số lượng còn bán được cho từng dòng hàng.");
        var actor=users.findByEmail(actorEmail).orElseThrow();
        for(var item:o.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).toList()) {
            Integer count=quantities.get(item.getId());
            if(count==null || count<0 || count>item.getQuantity()) throw new BusinessException("Số lượng hoàn kho phải từ 0 đến số đã mua.");
            Product p=item.getProduct(); refreshProduct(p);
            if((long)p.getStock()+count>Integer.MAX_VALUE) throw new BusinessException("Tồn kho vượt giới hạn.");
            p.setStock(p.getStock()+count); item.setRestockedQuantity(count);
            inventory.record(p,actor,o,StockMovementType.DELIVERY_RETURN,count,"Nhận hoàn SUNI-"+id,null);
        }
        o.setStatus(OrderStatus.RETURNED); o.setReturnedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        history.record(o,actorEmail,reason.strip());
    }
    @Transactional
    public void settleCod(Long id,String actorEmail) {
        CustomerOrder o=orders.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(o.getStatus()!=OrderStatus.COMPLETED) throw new BusinessException("Chỉ đối soát COD cho đơn giao thành công.");
        if(o.getCodSettledAt()!=null) return;
        o.setCodSettledAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        history.record(o,actorEmail,"Đã nhận đủ tiền COD: "+o.getTotal().toPlainString()+" VND");
    }
    private void cancel(CustomerOrder o,AppUser actor) {
        for (OrderItem item:o.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).toList()) {
            Product p=item.getProduct(); refreshProduct(p);
            if((long)p.getStock()+item.getQuantity()>Integer.MAX_VALUE) throw new BusinessException("Tồn kho vượt giới hạn; cần kiểm tra số dư trước khi hủy.");
            p.setStock(p.getStock()+item.getQuantity());
            inventory.record(p,actor,o,StockMovementType.CANCEL_RETURN,item.getQuantity(),"Hủy đơn SUNI-"+o.getId(),null);
        }
        o.setStatus(OrderStatus.CANCELLED);
    }
}
