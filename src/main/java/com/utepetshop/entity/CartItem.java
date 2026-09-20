package com.utepetshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
public class CartItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Cart cart;

  @ManyToOne(optional = false)
  private Product product;

  @ManyToOne(optional = false)
  private Branch branch;

  private int quantity;

  public CartItem(Cart c, Product p, Branch b, int q) {
    cart = c;
    product = p;
    branch = b;
    quantity = q;
  }
}
