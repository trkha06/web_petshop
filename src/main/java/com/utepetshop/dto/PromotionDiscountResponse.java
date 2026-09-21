// File: src/main/java/com/utepetshop/dto/PromotionDiscountResponse.java
package com.utepetshop.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionDiscountResponse {
  private boolean valid;
  private String code;
  private BigDecimal originalAmount;
  private BigDecimal discountAmount;
  private BigDecimal finalAmount;
  private String message;
}
