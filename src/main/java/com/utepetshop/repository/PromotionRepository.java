// File: src/main/java/com/utepetshop/repository/PromotionRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Promotion;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
  Optional<Promotion> findByCodeAndActiveTrue(String code);

  Optional<Promotion> findByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT promotion FROM Promotion promotion WHERE promotion.code = :code")
  Optional<Promotion> findByCodeForUpdate(@Param("code") String code);

  List<Promotion> findByActiveTrue();
}
