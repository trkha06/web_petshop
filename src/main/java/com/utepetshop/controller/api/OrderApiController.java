package com.utepetshop.controller.api;

import com.utepetshop.dto.CheckoutRequest;
import com.utepetshop.entity.CustomerOrder;
import com.utepetshop.repository.OrderRepository;
import com.utepetshop.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
  private final OrderRepository orders;
  private final CurrentUser current;
  private final StoreService store;

  public OrderApiController(OrderRepository orders, CurrentUser current, StoreService store) {
    this.orders = orders;
    this.current = current;
    this.store = store;
  }

  @GetMapping
  public List<CustomerOrder> list() {
    return orders.findByCustomerIdOrderByCreatedAtDesc(current.required().getId());
  }

  @PostMapping
  public ResponseEntity<CustomerOrder> checkout(@Valid @RequestBody CheckoutRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(store.checkout(current.required(), req.paymentMethod(), req.deliveryType()));
  }

  @PostMapping("/{id}/cancel")
  ResponseEntity<?> cancel(@PathVariable Long id) {
    store.cancelByCustomer(current.required(), id);
    return ResponseEntity.ok(Map.of("message", "Đã hủy đơn và hoàn tồn kho"));
  }
}
