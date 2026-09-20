package com.utepetshop.repository;

import com.utepetshop.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  Optional<CartItem> findByCartIdAndProductIdAndBranchId(
      Long cartId, Long productId, Long branchId);
}
