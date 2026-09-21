// File: src/main/java/com/utepetshop/service/ReviewService.java
package com.utepetshop.service;

import com.utepetshop.dto.ReviewRequest;
import com.utepetshop.dto.ReviewResponse;
import com.utepetshop.entity.*;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final ProductRepository productRepository;
  private final ServiceRepository serviceRepository;
  private final OrderRepository orderRepository;
  private final BookingRepository bookingRepository;

  @Transactional
  public ReviewResponse createReview(ReviewRequest req, User user) {
    if (req.getProductId() == null && req.getServiceId() == null) {
      throw new BusinessException("Đánh giá phải gắn với sản phẩm hoặc dịch vụ!");
    }

    Review review = new Review();
    review.setUser(user);
    review.setRating(Math.max(1, Math.min(5, req.getRating())));
    review.setComment(req.getComment());
    review.setCreatedAt(LocalDateTime.now());

    // 1. Nếu đánh giá Sản phẩm: Kiểm tra xem user có Order COMPLETED chứa sản phẩm này không
    if (req.getProductId() != null) {
      Product product =
          productRepository
              .findById(req.getProductId())
              .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm"));

      List<CustomerOrder> completedOrders =
          orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId()).stream()
              .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
              .filter(
                  o ->
                      o.getItems().stream()
                          .anyMatch(item -> item.getProduct().getId().equals(product.getId())))
              .toList();

      if (completedOrders.isEmpty()) {
        throw new BusinessException(
            "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công!");
      }

      review.setProduct(product);
      review.setOrder(completedOrders.get(0));
    }

    // 2. Nếu đánh giá Dịch vụ: Kiểm tra xem user có Booking COMPLETED cho dịch vụ này không
    if (req.getServiceId() != null) {
      com.utepetshop.entity.Service service =
          serviceRepository
              .findById(req.getServiceId())
              .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ"));

      boolean hasCompletedBooking =
          bookingRepository.existsByUserIdAndServiceIdAndStatus(
              user.getId(), service.getId(), BookingStatus.COMPLETED);

      if (!hasCompletedBooking) {
        throw new BusinessException(
            "Bạn chỉ có thể đánh giá dịch vụ sau khi đã trải nghiệm và hoàn thành lịch hẹn!");
      }

      review.setService(service);
    }

    Review saved = reviewRepository.save(review);
    return ReviewResponse.fromEntity(saved);
  }

  @Transactional(readOnly = true)
  public List<ReviewResponse> getProductReviews(Long productId) {
    return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
        .map(ReviewResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ReviewResponse> getServiceReviews(Long serviceId) {
    return reviewRepository.findByServiceIdOrderByCreatedAtDesc(serviceId).stream()
        .map(ReviewResponse::fromEntity)
        .toList();
  }
}
