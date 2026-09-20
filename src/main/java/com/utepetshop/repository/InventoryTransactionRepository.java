package com.utepetshop.repository;

import com.utepetshop.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {}
