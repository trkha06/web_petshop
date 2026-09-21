// File: src/main/java/com/utepetshop/controller/api/PromotionApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.ApplyPromotionRequest;
import com.utepetshop.dto.PromotionDiscountResponse;
import com.utepetshop.dto.PromotionDto;
import com.utepetshop.service.PromotionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionApiController {
  private final PromotionService promotionService;

  @PostMapping("/apply")
  public ResponseEntity<PromotionDiscountResponse> applyPromotion(
      @Valid @RequestBody ApplyPromotionRequest request) {
    return ResponseEntity.ok(
        promotionService.validateAndCalculate(request.getCode(), request.getOrderAmount()));
  }

  @GetMapping
  public ResponseEntity<List<PromotionDto>> getPromotions() {
    return ResponseEntity.ok(promotionService.getAllPromotions());
  }

  @PostMapping
  public ResponseEntity<PromotionDto> createPromotion(@Valid @RequestBody PromotionDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createPromotion(dto));
  }

  @PutMapping("/{id}/toggle")
  public ResponseEntity<Void> togglePromotion(@PathVariable Long id) {
    promotionService.togglePromotionActive(id);
    return ResponseEntity.ok().build();
  }
}
