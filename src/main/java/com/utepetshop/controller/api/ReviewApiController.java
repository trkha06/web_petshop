// File: src/main/java/com/utepetshop/controller/api/ReviewApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.ReviewRequest;
import com.utepetshop.dto.ReviewResponse;
import com.utepetshop.entity.User;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {
  private final ReviewService reviewService;
  private final CurrentUser currentUser;

  @PostMapping
  public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
    User user = currentUser.required();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(reviewService.createReview(request, user));
  }

  @GetMapping("/product/{productId}")
  public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable Long productId) {
    return ResponseEntity.ok(reviewService.getProductReviews(productId));
  }

  @GetMapping("/service/{serviceId}")
  public ResponseEntity<List<ReviewResponse>> getServiceReviews(@PathVariable Long serviceId) {
    return ResponseEntity.ok(reviewService.getServiceReviews(serviceId));
  }
}
