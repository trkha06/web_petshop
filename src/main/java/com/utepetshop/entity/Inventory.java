package com.utepetshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "inventories",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "branch_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Inventory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Product product;

  @ManyToOne(optional = false)
  private Branch branch;

  @Column(nullable = false)
  private int quantity;

  private int lowStockThreshold = 5;

  public Inventory(Product p, Branch b, int q) {
    product = p;
    branch = b;
    quantity = q;
  }
}
