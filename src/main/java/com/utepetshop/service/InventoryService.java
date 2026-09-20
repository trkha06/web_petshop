package com.utepetshop.service;

import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private final InventoryRepository inventories;
  private final InventoryTransactionRepository transactions;

  public InventoryService(InventoryRepository i, InventoryTransactionRepository t) {
    inventories = i;
    transactions = t;
  }

  @Transactional
  public void adjust(Long inventoryId, int delta, String note) {
    Inventory i =
        inventories
            .findById(inventoryId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho"));
    if (i.getQuantity() + delta < 0)
      throw new IllegalArgumentException("Điều chỉnh làm tồn kho âm");
    i.setQuantity(i.getQuantity() + delta);
    transactions.save(
        new InventoryTransaction(
            i,
            delta >= 0 ? InventoryTransactionType.IMPORT : InventoryTransactionType.ADJUSTMENT,
            delta,
            note));
  }
}
