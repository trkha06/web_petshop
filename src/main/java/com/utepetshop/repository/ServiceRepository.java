// File: src/main/java/com/utepetshop/repository/ServiceRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Service;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
  List<Service> findByActiveTrueOrderByIdAsc();

  long countByActiveTrue();
}
