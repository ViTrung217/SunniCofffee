package vn.sunicoffee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import vn.sunicoffee.domain.*;
import vn.sunicoffee.repository.*;
import vn.sunicoffee.web.form.*;

@Service
@Transactional(readOnly=true)
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryService inventory;
    private final UserRepository users;
    public CatalogService(ProductRepository products,CategoryRepository categories,InventoryService inventory,UserRepository users) { this.products=products; this.categories=categories; this.inventory=inventory; this.users=users; }
    public Product product(Long id) { return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    public Category category(Long id) { return categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    @Transactional
    public void saveCategory(Long id,CategoryForm f) {
        Long checkId=id==null ? -1L : id;
        if (categories.existsByCodeAndIdNot(f.getCode(),checkId) || categories.existsByNameAndIdNot(f.getName().strip(),checkId))
            throw new BusinessException("Mã hoặc tên danh mục đã tồn tại.");
        Category c=id==null ? new Category() : category(id);
        c.setCode(f.getCode()); c.setName(f.getName().strip()); c.setDescription(f.getDescription().strip()); categories.save(c);
    }
    @Transactional
    public void deleteCategory(Long id) {
        Category c=category(id);
        if (products.existsByCategoryId(id)) throw new BusinessException("Danh mục đang có sản phẩm; hãy chuyển sản phẩm sang danh mục khác trước.");
        categories.delete(c);
    }
    @Transactional
    public void saveProduct(Long id,ProductForm f) {
        saveProduct(id,f,null);
    }
    @Transactional
    public void saveProduct(Long id,ProductForm f,String actorEmail) {
        if (products.existsBySkuAndIdNot(f.getSku(),id==null ? -1L : id)) throw new BusinessException("Mã SKU đã tồn tại.");
        Product p=id==null ? new Product() : products.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (id!=null && !java.util.Objects.equals(p.getVersion(),f.getVersion()))
            throw new BusinessException("Sản phẩm hoặc tồn kho đã thay đổi. Hãy mở lại trang sửa để lấy dữ liệu mới nhất.");
        if(id!=null && p.getStock()!=f.getStock()) throw new BusinessException("Hãy sử dụng phiếu nhập/xuất kho để thay đổi tồn kho.");
        p.setSku(f.getSku()); p.setName(f.getName().strip()); p.setDescription(f.getDescription().strip());
        p.setPrice(f.getPrice()); p.setStock(f.getStock()); p.setSalesUnit(f.getSalesUnit().strip());
        p.setActive(f.isActive()); p.setCategory(category(f.getCategoryId())); products.save(p);
        if(id==null) inventory.record(p,actorEmail==null ? null : users.findByEmail(actorEmail).orElseThrow(),null,StockMovementType.OPENING,p.getStock(),"Tồn đầu khi tạo sản phẩm",null);
    }
    @Transactional
    public void archiveProduct(Long id) {
        Product p=products.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setActive(false);
    }
}
