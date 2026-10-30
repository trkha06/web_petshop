package com.utepetshop.repository;

import com.utepetshop.entity.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {
  @EntityGraph(attributePaths = {"items", "items.product", "items.branch"})
  Optional<Cart> findByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"items", "items.product", "items.branch"})
  @Query("SELECT cart FROM Cart cart WHERE cart.user.id = :userId")
  Optional<Cart> findByUserIdForCheckoutLock(@Param("userId") Long userId);
}
