// File: src/main/java/com/utepetshop/dto/BookingCreateRequest.java
package com.utepetshop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {
  @NotNull(message = "Vui lòng chọn thú cưng")
  private Long petId;

  @NotNull(message = "Vui lòng chọn dịch vụ")
  private Long serviceId;

  @NotNull(message = "Vui lòng chọn chi nhánh")
  private Long branchId;

  @NotNull(message = "Vui lòng chọn khung giờ hẹn")
  private Long slotId;

  private String note;
}
