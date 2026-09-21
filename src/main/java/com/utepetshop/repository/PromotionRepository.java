// File: src/main/java/com/utepetshop/repository/PromotionRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Promotion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
  Optional<Promotion> findByCodeAndActiveTrue(String code);

  Optional<Promotion> findByCode(String code);

  List<Promotion> findByActiveTrue();
}
