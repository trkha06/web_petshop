// File: src/main/java/com/utepetshop/dto/ApplyPromotionRequest.java
package com.utepetshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyPromotionRequest {
  @NotBlank(message = "Mã giảm giá không được để trống")
  private String code;

  @NotNull(message = "Số tiền đơn hàng không được để trống")
  private BigDecimal orderAmount;
}
