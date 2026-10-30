// File: src/main/java/com/utepetshop/repository/ServiceSlotRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.ServiceSlot;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceSlotRepository extends JpaRepository<ServiceSlot, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM ServiceSlot s WHERE s.id = :id")
  Optional<ServiceSlot> findByIdWithLock(@Param("id") Long id);

  List<ServiceSlot> findByBranchIdAndServiceIdAndSlotDateAndActiveTrueOrderByStartTimeAsc(
      Long branchId, Long serviceId, LocalDate slotDate);

  Optional<ServiceSlot> findByBranchIdAndServiceIdAndSlotDateAndStartTime(
      Long branchId, Long serviceId, LocalDate slotDate, LocalTime startTime);

  boolean existsByBranchIdAndServiceIdAndSlotDateAndStartTime(
      Long branchId, Long serviceId, LocalDate slotDate, LocalTime startTime);

  List<ServiceSlot> findByBranchIdAndSlotDateOrderByStartTimeAsc(Long branchId, LocalDate slotDate);

  @Query(
      "SELECT s FROM ServiceSlot s JOIN FETCH s.branch "
          + "WHERE s.slotDate >= :slotDate AND s.active = true "
          + "ORDER BY s.slotDate ASC, s.startTime ASC")
  List<ServiceSlot> findNextActiveSlots(@Param("slotDate") LocalDate slotDate);
}
