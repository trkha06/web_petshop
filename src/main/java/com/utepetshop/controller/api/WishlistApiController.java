// File: src/main/java/com/utepetshop/controller/api/WishlistApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.ProductResponse;
import com.utepetshop.entity.User;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.WishlistService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistApiController {
  private final WishlistService wishlistService;
  private final CurrentUser currentUser;

  @GetMapping
  public ResponseEntity<List<ProductResponse>> getWishlist() {
    User user = currentUser.required();
    return ResponseEntity.ok(wishlistService.getWishlistProducts(user));
  }

  @PostMapping("/{productId}")
  public ResponseEntity<Map<String, Object>> toggleWishlist(@PathVariable Long productId) {
    User user = currentUser.required();
    boolean added = wishlistService.toggleWishlist(productId, user);
    return ResponseEntity.ok(
        Map.of(
            "added", added,
            "message", added ? "Đã thêm vào danh sách yêu thích" : "Đã xóa khỏi danh sách yêu thích"));
  }
}
