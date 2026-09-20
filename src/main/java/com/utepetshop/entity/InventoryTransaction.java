package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Inventory inventory;

  @Enumerated(EnumType.STRING)
  private InventoryTransactionType type;

  private int quantityChange;
  private String note;
  private LocalDateTime createdAt = LocalDateTime.now();

  public InventoryTransaction(Inventory i, InventoryTransactionType t, int q, String n) {
    inventory = i;
    type = t;
    quantityChange = q;
    note = n;
  }
}
