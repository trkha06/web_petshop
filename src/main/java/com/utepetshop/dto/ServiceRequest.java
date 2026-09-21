// File: src/main/java/com/utepetshop/dto/ServiceRequest.java
package com.utepetshop.dto;

import com.utepetshop.entity.PetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {
  @NotBlank(message = "Tên dịch vụ không được để trống")
  private String name;

  private String description;

  @NotNull(message = "Giá dịch vụ không được để trống")
  @Min(value = 0, message = "Giá dịch vụ phải lớn hơn hoặc bằng 0")
  private BigDecimal price;

  @Min(value = 15, message = "Thời gian dịch vụ tối thiểu 15 phút")
  @Builder.Default
  private int durationMinutes = 60;

  private String imageUrl;
  private PetType petType;
}
