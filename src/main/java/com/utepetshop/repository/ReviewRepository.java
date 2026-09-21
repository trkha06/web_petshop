// File: src/main/java/com/utepetshop/repository/ReviewRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
  @EntityGraph(attributePaths = {"user", "product", "service"})
  List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

  @EntityGraph(attributePaths = {"user", "product", "service"})
  List<Review> findByServiceIdOrderByCreatedAtDesc(Long serviceId);

  @EntityGraph(attributePaths = {"user", "product", "service"})
  List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  boolean existsByUserIdAndServiceId(Long userId, Long serviceId);
}
