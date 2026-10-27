package com.utepetshop.repository;

import com.utepetshop.entity.Branch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
  List<Branch> findByActiveTrueOrderByNameAsc();
}
