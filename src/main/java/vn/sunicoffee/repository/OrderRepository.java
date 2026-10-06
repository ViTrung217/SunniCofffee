package vn.sunicoffee.repository;
import vn.sunicoffee.domain.CustomerOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<CustomerOrder,Long> {
    Page<CustomerOrder> findByUserEmailOrderByCreatedAtDesc(String email,Pageable pageable);
    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @Query("select o from CustomerOrder o where (:status is null or o.status=:status) and (:from is null or o.createdAt>=:from) and (:until is null or o.createdAt<:until) and (:cutoff is null or (o.status=vn.sunicoffee.domain.OrderStatus.PENDING and o.createdAt<:cutoff)) and (:q='' or lower(o.recipientName) like lower(concat('%',:q,'%')) or lower(o.user.email) like lower(concat('%',:q,'%')) or cast(o.id as string)=:q) order by o.createdAt desc,o.id desc")
    Page<CustomerOrder> search(@Param("q") String q,@Param("status") vn.sunicoffee.domain.OrderStatus status,
        @Param("from") java.time.LocalDateTime from,@Param("until") java.time.LocalDateTime until,
        @Param("cutoff") java.time.LocalDateTime cutoff,Pageable pageable);
    Optional<CustomerOrder> findByCheckoutToken(String token);
    @EntityGraph(attributePaths={"items", "items.product"})
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> detail(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> lockById(@Param("id") Long id);
}
