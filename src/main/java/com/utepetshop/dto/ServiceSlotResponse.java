// File: src/main/java/com/utepetshop/dto/ServiceSlotResponse.java
package com.utepetshop.dto;

import com.utepetshop.entity.ServiceSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceSlotResponse {
  private Long id;
  private Long branchId;
  private String branchName;
  private Long serviceId;
  private String serviceName;
  private LocalDate slotDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private int capacity;
  private int bookedCount;
  private int available;
  private boolean active;

  public static ServiceSlotResponse fromEntity(ServiceSlot slot) {
    int avail = Math.max(0, slot.getCapacity() - slot.getBookedCount());
    return ServiceSlotResponse.builder()
        .id(slot.getId())
        .branchId(slot.getBranch() != null ? slot.getBranch().getId() : null)
        .branchName(slot.getBranch() != null ? slot.getBranch().getName() : null)
        .serviceId(slot.getService() != null ? slot.getService().getId() : null)
        .serviceName(slot.getService() != null ? slot.getService().getName() : null)
        .slotDate(slot.getSlotDate())
        .startTime(slot.getStartTime())
        .endTime(slot.getEndTime())
        .capacity(slot.getCapacity())
        .bookedCount(slot.getBookedCount())
        .available(avail)
        .active(slot.isActive())
        .build();
  }
}
