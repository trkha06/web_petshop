// File: src/main/java/com/utepetshop/dto/ReviewRequest.java
package com.utepetshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {
  private Long productId;
  private Long serviceId;
  private Long orderId;
  private Long bookingId;

  @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
  @Max(value = 5, message = "Đánh giá tối đa 5 sao")
  @Builder.Default
  private int rating = 5;

  @NotBlank(message = "Nội dung nhận xét không được để trống")
  private String comment;
}
