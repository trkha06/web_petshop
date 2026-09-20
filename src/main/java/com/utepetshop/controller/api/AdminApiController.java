package com.utepetshop.controller.api;

import com.utepetshop.dto.ProductResponse;
import com.utepetshop.repository.ProductRepository;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminApiController {
  private final ProductRepository products;

  public AdminApiController(ProductRepository products) {
    this.products = products;
  }

  @GetMapping
  public List<ProductResponse> list() {
    return products.findAll().stream().map(ProductResponse::of).toList();
  }
}
