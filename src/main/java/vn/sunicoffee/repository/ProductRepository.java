package vn.sunicoffee.repository;
import vn.sunicoffee.domain.Product;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,Long> {
    @Query("select p from Product p where (:admin = true or p.active = true) and (:category is null or p.category.id = :category) and (lower(p.name) like lower(concat('%', :q, '%')) or lower(p.sku) like lower(concat('%', :q, '%')))")
    Page<Product> search(@Param("q") String q,@Param("category") Long category,@Param("admin") boolean admin,Pageable pageable);
    boolean existsBySkuAndIdNot(String sku,Long id);
    boolean existsByCategoryId(Long categoryId);
    Optional<Product> findBySku(String sku);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> lockById(@Param("id") Long id);
}
