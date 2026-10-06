package vn.sunicoffee.repository;
import vn.sunicoffee.domain.StockMovement;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface StockMovementRepository extends JpaRepository<StockMovement,Long> {
    Optional<StockMovement> findByRequestToken(String token);
    @Query("select m from StockMovement m where (:productId is null or m.product.id=:productId)")
    Page<StockMovement> history(@Param("productId") Long productId,Pageable pageable);
}
