package vn.sunicoffee.repository;
import vn.sunicoffee.domain.AppUser;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface UserRepository extends JpaRepository<AppUser,Long> {
    @Query("select u from AppUser u where lower(u.fullName) like lower(concat('%',:q,'%')) or lower(u.email) like lower(concat('%',:q,'%'))")
    org.springframework.data.domain.Page<AppUser> search(@Param("q") String q,org.springframework.data.domain.Pageable pageable);
    long countByRoleAndEnabledTrue(vn.sunicoffee.domain.Role role);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> lockById(@Param("id") Long id);
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.email = :email")
    Optional<AppUser> lockByEmail(@Param("email") String email);
}
