package vn.sunicoffee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly=true)
public class CartService {
    private final UserRepository users;
    private final ProductRepository products;
    private final CartRepository carts;
    public CartService(UserRepository u,ProductRepository p,CartRepository c) { users=u; products=p; carts=c; }
    public List<CartItem> items(String email) {
        return carts.findByUserIdOrderByProductIdAsc(users.findByEmail(email).orElseThrow().getId());
    }
    public BigDecimal total(List<CartItem> items) {
        return items.stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    @Transactional
    public void add(String email,Long productId,int quantity) { change(email,productId,quantity,true); }
    @Transactional
    public void update(String email,Long productId,int quantity) { change(email,productId,quantity,false); }
    private void change(String email,Long productId,int quantity,boolean add) {
        if (quantity<1 || quantity>99) throw new BusinessException("Số lượng cần từ 1 đến 99.");
        AppUser user=users.lockByEmail(email).orElseThrow();
        Product p=products.lockById(productId).orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm."));
        CartItem item=carts.findByUserIdAndProductId(user.getId(),productId).orElseGet(CartItem::new);
        if (!add && item.getId()==null) throw new BusinessException("Sản phẩm không có trong giỏ.");
        int wanted=add ? item.getQuantity()+quantity : quantity;
        if (!p.isActive()) throw new BusinessException("Sản phẩm đã ngừng bán.");
        if (wanted>99 || wanted>p.getStock()) throw new BusinessException("Số lượng vượt tồn kho hoặc giới hạn 99 sản phẩm mỗi dòng.");
        item.setUser(user); item.setProduct(p); item.setQuantity(wanted); carts.save(item);
    }
    @Transactional
    public void remove(String email,Long productId) {
        AppUser u=users.lockByEmail(email).orElseThrow();
        carts.findByUserIdAndProductId(u.getId(),productId).ifPresent(carts::delete);
    }
}
