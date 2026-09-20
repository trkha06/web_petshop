package com.utepetshop.repository;

import com.utepetshop.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CartRepository extends JpaRepository<Cart, Long> {
  @EntityGraph(attributePaths = {"items", "items.product", "items.branch"})
  Optional<Cart> findByUserId(Long userId);
}
