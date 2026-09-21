// File: src/main/java/com/utepetshop/repository/BookingRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Booking;
import com.utepetshop.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

  @EntityGraph(attributePaths = {"user", "pet", "service", "branch", "slot"})
  List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

  @EntityGraph(attributePaths = {"user", "pet", "service", "branch", "slot"})
  List<Booking> findByBranchIdOrderByCreatedAtDesc(Long branchId);

  @EntityGraph(attributePaths = {"user", "pet", "service", "branch", "slot"})
  List<Booking> findAllByOrderByCreatedAtDesc();

  @EntityGraph(attributePaths = {"user", "pet", "service", "branch", "slot"})
  Optional<Booking> findByCode(String code);

  long countByStatus(BookingStatus status);

  boolean existsByUserIdAndServiceIdAndStatus(Long userId, Long serviceId, BookingStatus status);

  boolean existsByUserIdAndSlotIdAndStatusNot(Long userId, Long slotId, BookingStatus status);

  // Analytics queries
  @Query(
      "SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = com.utepetshop.entity.BookingStatus.COMPLETED AND b.createdAt >= :since")
  BigDecimal sumRevenueSince(@Param("since") LocalDateTime since);

  @Query(
      "SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = com.utepetshop.entity.BookingStatus.COMPLETED AND b.createdAt BETWEEN :start AND :end")
  BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  @Query("SELECT b.status, COUNT(b) FROM Booking b GROUP BY b.status")
  List<Object[]> countBookingsByStatus();

  @Query(
      "SELECT b.service.id, b.service.name, COUNT(b), COALESCE(SUM(b.totalPrice), 0) "
          + "FROM Booking b WHERE b.status = com.utepetshop.entity.BookingStatus.COMPLETED "
          + "GROUP BY b.service.id, b.service.name ORDER BY COUNT(b) DESC")
  List<Object[]> findTopServices();

  List<Booking> findByStatusAndCreatedAtGreaterThanEqual(
      BookingStatus status, LocalDateTime createdAt);
}
