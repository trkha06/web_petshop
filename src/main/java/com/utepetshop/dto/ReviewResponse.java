// File: src/main/java/com/utepetshop/dto/ReviewResponse.java
package com.utepetshop.dto;

import com.utepetshop.entity.Review;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
  private Long id;
  private Long userId;
  private String userFullName;
  private Long productId;
  private String productName;
  private Long serviceId;
  private String serviceName;
  private int rating;
  private String comment;
  private LocalDateTime createdAt;

  public static ReviewResponse fromEntity(Review r) {
    return ReviewResponse.builder()
        .id(r.getId())
        .userId(r.getUser() != null ? r.getUser().getId() : null)
        .userFullName(r.getUser() != null ? r.getUser().getFullName() : null)
        .productId(r.getProduct() != null ? r.getProduct().getId() : null)
        .productName(r.getProduct() != null ? r.getProduct().getName() : null)
        .serviceId(r.getService() != null ? r.getService().getId() : null)
        .serviceName(r.getService() != null ? r.getService().getName() : null)
        .rating(r.getRating())
        .comment(r.getComment())
        .createdAt(r.getCreatedAt())
        .build();
  }
}
