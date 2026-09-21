// File: src/main/java/com/utepetshop/dto/SlotGenerateRequest.java
package com.utepetshop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotGenerateRequest {
  @NotNull(message = "Chi nhánh không được để trống")
  private Long branchId;

  @NotNull(message = "Dịch vụ không được để trống")
  private Long serviceId;

  @NotNull(message = "Ngày bắt đầu không được để trống")
  private LocalDate startDate;

  @NotNull(message = "Ngày kết thúc không được để trống")
  private LocalDate endDate;

  @NotNull(message = "Giờ mở cửa không được để trống")
  private LocalTime startTime;

  @NotNull(message = "Giờ đóng cửa không được để trống")
  private LocalTime endTime;

  @Min(value = 1, message = "Số lượng chỗ tối thiểu là 1")
  @Builder.Default
  private int capacity = 1;

  @Min(value = 15, message = "Khoảng thời gian tối thiểu 15 phút")
  @Builder.Default
  private int intervalMinutes = 60;
}
