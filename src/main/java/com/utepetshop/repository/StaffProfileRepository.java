// File: src/main/java/com/utepetshop/repository/StaffProfileRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.StaffProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
  @EntityGraph(attributePaths = {"user", "branch"})
  Optional<StaffProfile> findByUserId(Long userId);

  @EntityGraph(attributePaths = {"user", "branch"})
  List<StaffProfile> findByBranchId(Long branchId);
}
