// File: src/main/java/com/utepetshop/dto/BookingResponse.java
package com.utepetshop.dto;

import com.utepetshop.entity.Booking;
import com.utepetshop.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
  private Long id;
  private String code;
  private Long userId;
  private String userFullName;
  private String userEmail;
  private Long petId;
  private String petName;
  private Long serviceId;
  private String serviceName;
  private Long branchId;
  private String branchName;
  private Long slotId;
  private LocalDate slotDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private BigDecimal totalPrice;
  private String note;
  private BookingStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static BookingResponse fromEntity(Booking b) {
    return BookingResponse.builder()
        .id(b.getId())
        .code(b.getCode())
        .userId(b.getUser() != null ? b.getUser().getId() : null)
        .userFullName(b.getUser() != null ? b.getUser().getFullName() : null)
        .userEmail(b.getUser() != null ? b.getUser().getEmail() : null)
        .petId(b.getPet() != null ? b.getPet().getId() : null)
        .petName(b.getPet() != null ? b.getPet().getName() : null)
        .serviceId(b.getService() != null ? b.getService().getId() : null)
        .serviceName(b.getService() != null ? b.getService().getName() : null)
        .branchId(b.getBranch() != null ? b.getBranch().getId() : null)
        .branchName(b.getBranch() != null ? b.getBranch().getName() : null)
        .slotId(b.getSlot() != null ? b.getSlot().getId() : null)
        .slotDate(b.getSlot() != null ? b.getSlot().getSlotDate() : null)
        .startTime(b.getSlot() != null ? b.getSlot().getStartTime() : null)
        .endTime(b.getSlot() != null ? b.getSlot().getEndTime() : null)
        .totalPrice(b.getTotalPrice())
        .note(b.getNote())
        .status(b.getStatus())
        .createdAt(b.getCreatedAt())
        .updatedAt(b.getUpdatedAt())
        .build();
  }
}
