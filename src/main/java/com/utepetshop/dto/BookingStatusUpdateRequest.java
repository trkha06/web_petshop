// File: src/main/java/com/utepetshop/dto/BookingStatusUpdateRequest.java
package com.utepetshop.dto;

import com.utepetshop.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusUpdateRequest {
  @NotNull(message = "Trạng thái không được để trống")
  private BookingStatus status;

  private String note;
}
