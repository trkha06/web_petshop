package com.utepetshop.controller.api;

import com.utepetshop.dto.ProductResponse;
import com.utepetshop.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {
  private final ProductRepository products;

  public ProductApiController(ProductRepository products) {
    this.products = products;
  }

  @GetMapping
  public Page<ProductResponse> list(
      @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page) {
    return products
        .search(
            q == null || q.isBlank() ? null : q,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            PageRequest.of(Math.max(page, 0), 12, Sort.by("createdAt").descending()))
        .map(ProductResponse::of);
  }

  @GetMapping("/{id}")
  public ProductResponse detail(@PathVariable Long id) {
    return ProductResponse.of(products.findById(id).orElseThrow());
  }
}
