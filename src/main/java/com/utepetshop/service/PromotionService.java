// File: src/main/java/com/utepetshop/service/PromotionService.java
package com.utepetshop.service;

import com.utepetshop.dto.PromotionDiscountResponse;
import com.utepetshop.dto.PromotionDto;
import com.utepetshop.entity.DiscountType;
import com.utepetshop.entity.Promotion;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.PromotionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {
  private final PromotionRepository promotionRepository;

  @Transactional(readOnly = true)
  public PromotionDiscountResponse validateAndCalculate(String code, BigDecimal orderAmount) {
    if (code == null || code.isBlank()) {
      return PromotionDiscountResponse.builder()
          .valid(false)
          .message("Mã khuyến mãi không hợp lệ")
          .build();
    }

    Promotion promo =
        promotionRepository
            .findByCodeAndActiveTrue(code.trim().toUpperCase())
            .orElse(null);

    if (promo == null) {
      return PromotionDiscountResponse.builder()
          .valid(false)
          .code(code)
          .message("Mã khuyến mãi không tồn tại hoặc đã hết hạn")
          .build();
    }

    LocalDateTime now = LocalDateTime.now();
    if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
      return PromotionDiscountResponse.builder()
          .valid(false)
          .code(code)
          .message("Chương trình khuyến mãi chưa bắt đầu")
          .build();
    }

    if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
      return PromotionDiscountResponse.builder()
          .valid(false)
          .code(code)
          .message("Chương trình khuyến mãi đã kết thúc")
          .build();
    }

    if (promo.getUsedCount() >= promo.getUsageLimit()) {
      return PromotionDiscountResponse.builder()
          .valid(false)
          .code(code)
          .message("Mã khuyến mãi đã hết lượt sử dụng")
          .build();
    }

    if (promo.getMinOrderValue() != null && orderAmount.compareTo(promo.getMinOrderValue()) < 0) {
      return PromotionDiscountResponse.builder()
          .valid(false)
          .code(code)
          .message("Đơn hàng tối thiểu phải từ " + promo.getMinOrderValue() + "đ để dùng mã này")
          .build();
    }

    BigDecimal discount;
    if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
      discount =
          orderAmount
              .multiply(promo.getDiscountValue())
              .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
      if (promo.getMaxDiscountAmount() != null
          && discount.compareTo(promo.getMaxDiscountAmount()) > 0) {
        discount = promo.getMaxDiscountAmount();
      }
    } else {
      discount = promo.getDiscountValue();
      if (discount.compareTo(orderAmount) > 0) {
        discount = orderAmount;
      }
    }

    BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);

    return PromotionDiscountResponse.builder()
        .valid(true)
        .code(promo.getCode())
        .originalAmount(orderAmount)
        .discountAmount(discount)
        .finalAmount(finalAmount)
        .message("Áp dụng mã giảm giá thành công!")
        .build();
  }

  @Transactional
  public void usePromotion(String code) {
    if (code == null || code.isBlank()) return;
    promotionRepository
        .findByCode(code.trim().toUpperCase())
        .ifPresent(
            p -> {
              p.setUsedCount(p.getUsedCount() + 1);
              promotionRepository.save(p);
            });
  }

  @Transactional(readOnly = true)
  public List<PromotionDto> getAllPromotions() {
    return promotionRepository.findAll().stream().map(PromotionDto::fromEntity).toList();
  }

  @Transactional
  public PromotionDto createPromotion(PromotionDto dto) {
    if (dto.getCode() == null || dto.getCode().isBlank()) {
      throw new BusinessException("Mã khuyến mãi không được để trống!");
    }
    if (dto.getDiscountType() == null
        || dto.getDiscountValue() == null
        || dto.getDiscountValue().signum() <= 0) {
      throw new BusinessException("Loại và giá trị giảm giá không hợp lệ!");
    }
    if (dto.getDiscountType() == DiscountType.PERCENTAGE
        && dto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new BusinessException("Giảm giá phần trăm không được vượt quá 100%!");
    }
    if (dto.getStartDate() != null
        && dto.getEndDate() != null
        && !dto.getStartDate().isBefore(dto.getEndDate())) {
      throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu!");
    }
    if (dto.getUsageLimit() <= 0) {
      throw new BusinessException("Giới hạn sử dụng phải lớn hơn 0!");
    }
    if (promotionRepository.findByCode(dto.getCode().trim().toUpperCase()).isPresent()) {
      throw new BusinessException("Mã khuyến mãi này đã tồn tại!");
    }

    Promotion p = new Promotion();
    p.setCode(dto.getCode().trim().toUpperCase());
    p.setDiscountType(dto.getDiscountType());
    p.setDiscountValue(dto.getDiscountValue());
    p.setMinOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue() : BigDecimal.ZERO);
    p.setMaxDiscountAmount(dto.getMaxDiscountAmount());
    p.setStartDate(dto.getStartDate());
    p.setEndDate(dto.getEndDate());
    p.setUsageLimit(dto.getUsageLimit() > 0 ? dto.getUsageLimit() : 100);
    p.setUsedCount(0);
    p.setActive(true);

    Promotion saved = promotionRepository.save(p);
    return PromotionDto.fromEntity(saved);
  }

  @Transactional
  public void togglePromotionActive(Long id) {
    Promotion p =
        promotionRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy khuyến mãi #" + id));
    p.setActive(!p.isActive());
    promotionRepository.save(p);
  }
}
