// File: src/main/java/com/utepetshop/repository/PetRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Pet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
  List<Pet> findByUserIdAndActiveTrueOrderByIdDesc(Long userId);

  List<Pet> findByUserIdOrderByIdDesc(Long userId);

  Optional<Pet> findByIdAndUserId(Long id, Long userId);

  long countByActiveTrue();
}
