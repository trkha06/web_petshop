package com.utepetshop.service;

import com.utepetshop.dto.ProductResponse;
import com.utepetshop.entity.Product;
import com.utepetshop.entity.User;
import com.utepetshop.entity.Wishlist;
import com.utepetshop.entity.WishlistItem;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.ProductRepository;
import com.utepetshop.repository.WishlistItemRepository;
import com.utepetshop.repository.WishlistRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {
  private final WishlistRepository wishlistRepository;
  private final WishlistItemRepository wishlistItemRepository;
  private final ProductRepository productRepository;

  @Transactional
  public boolean toggleWishlist(Long productId, User user) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm"));

    Wishlist wishlist =
        wishlistRepository
            .findByUserId(user.getId())
            .orElseGet(() -> wishlistRepository.save(new Wishlist(user)));

    Optional<WishlistItem> itemOpt =
        wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId);

    if (itemOpt.isPresent()) {
      WishlistItem item = itemOpt.get();
      wishlist.getItems().removeIf(it -> it.getProduct().getId().equals(productId));
      wishlistItemRepository.delete(item);
      wishlistItemRepository.flush();
      return false;
    } else {
      WishlistItem newItem = new WishlistItem(wishlist, product);
      wishlist.getItems().add(newItem);
      wishlistItemRepository.save(newItem);
      wishlistItemRepository.flush();
      return true;
    }
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> getWishlistProducts(User user) {
    return wishlistRepository
        .findByUserId(user.getId())
        .map(
            w ->
                wishlistItemRepository.findByWishlistId(w.getId()).stream()
                    .map(WishlistItem::getProduct)
                    .map(ProductResponse::of)
                    .toList())
        .orElse(Collections.emptyList());
  }

  @Transactional(readOnly = true)
  public boolean isInWishlist(Long productId, User user) {
    return wishlistRepository
        .findByUserId(user.getId())
        .map(w -> wishlistItemRepository.existsByWishlistIdAndProductId(w.getId(), productId))
        .orElse(false);
  }
}
