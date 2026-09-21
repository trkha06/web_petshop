// File: src/main/java/com/utepetshop/controller/web/WishlistWebController.java
package com.utepetshop.controller.web;

import com.utepetshop.entity.User;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistWebController {
  private final WishlistService wishlistService;
  private final CurrentUser currentUser;

  @GetMapping
  public String list(Model model) {
    User user = currentUser.required();
    model.addAttribute("products", wishlistService.getWishlistProducts(user));
    return "wishlist/index";
  }

  @PostMapping("/{productId}/toggle")
  public String toggle(@PathVariable Long productId, RedirectAttributes ra) {
    try {
      User user = currentUser.required();
      boolean added = wishlistService.toggleWishlist(productId, user);
      ra.addFlashAttribute(
          "success",
          added ? "Đã thêm sản phẩm vào danh sách yêu thích" : "Đã xóa sản phẩm khỏi danh sách yêu thích");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/wishlist";
  }
}
