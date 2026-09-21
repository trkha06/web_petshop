// File: src/main/java/com/utepetshop/repository/WishlistRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Wishlist;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
  @EntityGraph(attributePaths = {"items", "items.product"})
  Optional<Wishlist> findByUserId(Long userId);
}
