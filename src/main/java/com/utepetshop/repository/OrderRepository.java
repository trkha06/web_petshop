package com.utepetshop.repository;

import com.utepetshop.entity.CustomerOrder;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
  @EntityGraph(attributePaths = {"items", "items.product", "branch", "customer"})
  List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long id);

  @EntityGraph(attributePaths = {"items", "items.product", "branch", "customer"})
  List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
