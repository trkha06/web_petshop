// File: src/main/java/com/utepetshop/entity/WishlistItem.java
package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "wishlist_items",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_wishlist_product",
          columnNames = {"wishlist_id", "product_id"})
    })
@Getter
@Setter
@NoArgsConstructor
public class WishlistItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wishlist_id", nullable = false)
  private Wishlist wishlist;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  private LocalDateTime createdAt = LocalDateTime.now();

  public WishlistItem(Wishlist wishlist, Product product) {
    this.wishlist = wishlist;
    this.product = product;
    this.createdAt = LocalDateTime.now();
  }
}
