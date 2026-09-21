package com.utepetshop.repository;

import com.utepetshop.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
  Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Inventory i where i.product.id=:productId and i.branch.id=:branchId")
  Optional<Inventory> lockByProductAndBranch(
      @Param("productId") Long productId, @Param("branchId") Long branchId);

  List<Inventory> findByProductId(Long productId);

  long countByQuantityLessThanEqual(int threshold);
}
