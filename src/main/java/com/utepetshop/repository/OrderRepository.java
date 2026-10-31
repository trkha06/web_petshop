package com.utepetshop.repository;

import com.utepetshop.entity.CustomerOrder;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
  @EntityGraph(attributePaths = {"items", "items.product", "branch", "customer"})
  List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long id);

  @EntityGraph(attributePaths = {"items", "items.product", "branch", "customer"})
  List<CustomerOrder> findAllByOrderByCreatedAtDesc();

  @EntityGraph(attributePaths = {"items", "items.product", "branch", "customer"})
  List<CustomerOrder> findByBranchIdOrderByCreatedAtDesc(Long branchId);

  @EntityGraph(attributePaths = {"items", "items.product", "branch", "customer"})
  Optional<CustomerOrder> findByIdAndCustomerId(Long id, Long customerId);

  @EntityGraph(attributePaths = {"items", "items.product", "branch", "customer"})
  Optional<CustomerOrder> findByCheckoutRequestKey(String checkoutRequestKey);

  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status = com.utepetshop.entity.OrderStatus.COMPLETED AND o.createdAt >= :since")
  java.math.BigDecimal sumRevenueSince(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status = com.utepetshop.entity.OrderStatus.COMPLETED AND o.createdAt BETWEEN :start AND :end")
  java.math.BigDecimal sumRevenueBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

  @Query("SELECT o.status, COUNT(o) FROM CustomerOrder o GROUP BY o.status")
  List<Object[]> countOrdersByStatus();

  @Query(
      "SELECT oi.product.id, oi.productName, SUM(oi.quantity), COALESCE(SUM(oi.unitPrice * oi.quantity), 0) "
          + "FROM OrderItem oi WHERE oi.order.status = com.utepetshop.entity.OrderStatus.COMPLETED "
          + "GROUP BY oi.product.id, oi.productName ORDER BY SUM(oi.quantity) DESC")
  List<Object[]> findTopProducts();

  List<CustomerOrder> findByStatusAndCreatedAtGreaterThanEqual(
      com.utepetshop.entity.OrderStatus status, java.time.LocalDateTime createdAt);
}
