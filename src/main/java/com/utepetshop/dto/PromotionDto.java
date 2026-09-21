// File: src/main/java/com/utepetshop/dto/PromotionDto.java
package com.utepetshop.dto;

import com.utepetshop.entity.DiscountType;
import com.utepetshop.entity.Promotion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionDto {
  private Long id;
  private String code;
  private DiscountType discountType;
  private BigDecimal discountValue;
  private BigDecimal minOrderValue;
  private BigDecimal maxDiscountAmount;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private int usageLimit;
  private int usedCount;
  private boolean active;

  public static PromotionDto fromEntity(Promotion p) {
    return PromotionDto.builder()
        .id(p.getId())
        .code(p.getCode())
        .discountType(p.getDiscountType())
        .discountValue(p.getDiscountValue())
        .minOrderValue(p.getMinOrderValue())
        .maxDiscountAmount(p.getMaxDiscountAmount())
        .startDate(p.getStartDate())
        .endDate(p.getEndDate())
        .usageLimit(p.getUsageLimit())
        .usedCount(p.getUsedCount())
        .active(p.isActive())
        .build();
  }
}
