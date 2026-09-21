// File: src/main/java/com/utepetshop/entity/ServiceSlot.java
package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;

@Entity
@Table(
    name = "service_slots",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_slot_branch_service_date_time",
          columnNames = {"branch_id", "service_id", "slot_date", "start_time"})
    })
@Getter
@Setter
@NoArgsConstructor
public class ServiceSlot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_id", nullable = false)
  private Service service;

  @Column(name = "slot_date", nullable = false)
  private LocalDate slotDate;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(nullable = false)
  private int capacity = 1;

  @Column(name = "booked_count", nullable = false)
  private int bookedCount = 0;

  private boolean active = true;

  public ServiceSlot(
      Branch branch,
      Service service,
      LocalDate slotDate,
      LocalTime startTime,
      LocalTime endTime,
      int capacity) {
    this.branch = branch;
    this.service = service;
    this.slotDate = slotDate;
    this.startTime = startTime;
    this.endTime = endTime;
    this.capacity = capacity;
    this.bookedCount = 0;
    this.active = true;
  }
}
