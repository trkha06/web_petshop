package com.utepetshop.repository;

import com.utepetshop.entity.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
  @EntityGraph(attributePaths = {"product", "product.brand", "product.category"})
  List<WishlistItem> findByWishlistId(Long wishlistId);

  Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);

  boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

  void deleteByWishlistIdAndProductId(Long wishlistId, Long productId);
}
