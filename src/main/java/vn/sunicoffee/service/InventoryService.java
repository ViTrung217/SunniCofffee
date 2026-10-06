package vn.sunicoffee.service;

import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.web.form.StockForm;

@Service
public class InventoryService {
    private final ProductRepository products;
    private final UserRepository users;
    private final StockMovementRepository movements;
    public InventoryService(ProductRepository p,UserRepository u,StockMovementRepository m) {products=p;users=u;movements=m;}
    @Transactional public void manual(String email,StockForm f) {
        var actor=users.findByEmail(email).orElseThrow();
        if(!actor.isEnabled() || actor.getRole()!=Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(f.getType()!=StockMovementType.RECEIPT && f.getType()!=StockMovementType.ISSUE)
            throw new BusinessException("Phiếu thủ công chỉ hỗ trợ nhập kho hoặc xuất kho khác.");
        if(f.getQuantity()==null || f.getQuantity()<1 || f.getQuantity()>1000000) throw new BusinessException("Số lượng cần từ 1 đến 1.000.000.");
        var product=products.lockById(f.getProductId()).orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm."));
        var previous=movements.findByRequestToken(f.getRequestToken());
        if(previous.isPresent()) {
            var p=previous.get();
            if(!p.getProduct().getId().equals(product.getId()) || p.getType()!=f.getType() || Math.abs(p.getQuantityDelta())!=f.getQuantity()
                    || !p.getActor().getId().equals(actor.getId()) || !p.getReason().equals(f.getReason().strip()))
                throw new BusinessException("Mã phiếu đã được dùng cho giao dịch khác.");
            return;
        }
        int delta=f.getType()==StockMovementType.RECEIPT ? f.getQuantity() : -f.getQuantity();
        long balance=(long)product.getStock()+delta;
        if(balance<0) throw new BusinessException("Số lượng xuất vượt tồn kho hiện tại.");
        if(balance>Integer.MAX_VALUE) throw new BusinessException("Số lượng tồn vượt giới hạn cho phép.");
        product.setStock((int)balance);
        record(product,actor,null,f.getType(),delta,f.getReason().strip(),f.getRequestToken());
    }
    /** Caller changes stock under its product lock; ledger participates in that same transaction. */
    @Transactional public void record(Product product,AppUser actor,CustomerOrder order,StockMovementType type,int delta,String reason,String token) {
        if(delta==0) return;
        var m=new StockMovement(); m.setProduct(product);m.setActor(actor);m.setOrder(order);m.setType(type);
        m.setQuantityDelta(delta);m.setBalanceAfter(product.getStock());m.setReason(reason);m.setRequestToken(token);
        m.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));movements.save(m);
    }
}
