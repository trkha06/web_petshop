package com.utepetshop.controller.api;

import com.utepetshop.dto.*;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class ManagerCatalogApiController {
  private final CatalogService catalog;
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final BrandRepository brands;
  private final CloudinaryImageService cloudinary;

  public ManagerCatalogApiController(
      CatalogService c,
      ProductRepository p,
      CategoryRepository ca,
      BrandRepository b,
      CloudinaryImageService ci) {
    catalog = c;
    products = p;
    categories = ca;
    brands = b;
    cloudinary = ci;
  }

  @PostMapping("/products")
  ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest r) {
    return ResponseEntity.status(201).body(ProductResponse.of(catalog.save(null, r)));
  }

  @PutMapping("/products/{id}")
  ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest r) {
    return ProductResponse.of(catalog.save(id, r));
  }

  @DeleteMapping("/products/{id}")
  ResponseEntity<Void> delete(@PathVariable Long id) {
    catalog.deactivate(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<?> image(@PathVariable Long id, @RequestPart MultipartFile file) {
    ProductImage image = cloudinary.upload(id, file);
    return ResponseEntity.status(201)
        .body(Map.of("id", image.getId(), "secureUrl", image.getSecureUrl()));
  }

  @GetMapping("/categories")
  List<Category> categories() {
    return categories.findAll();
  }

  @PostMapping("/categories")
  Category category(@RequestBody Map<String, String> b) {
    return categories.save(new Category(b.get("name")));
  }

  @GetMapping("/brands")
  List<Brand> brands() {
    return brands.findAll();
  }

  @PostMapping("/brands")
  Brand brand(@RequestBody Map<String, String> b) {
    return brands.save(new Brand(b.get("name")));
  }
}
