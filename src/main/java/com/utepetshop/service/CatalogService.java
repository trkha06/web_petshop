package com.utepetshop.service;

import com.utepetshop.dto.ProductRequest;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final BrandRepository brands;

  public CatalogService(ProductRepository p, CategoryRepository c, BrandRepository b) {
    products = p;
    categories = c;
    brands = b;
  }

  @Transactional
  public Product save(Long id, ProductRequest req) {
    Product p =
        id == null
            ? new Product()
            : products
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    p.setSku(req.sku());
    p.setName(req.name());
    p.setDescription(req.description());
    p.setPrice(req.price());
    p.setCategory(
        categories
            .findById(req.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại")));
    p.setBrand(
        brands
            .findById(req.brandId())
            .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại")));
    p.setPetType(req.petType());
    p.setImageUrl(req.imageUrl());
    return products.save(p);
  }

  @Transactional
  public void deactivate(Long id) {
    Product p =
        products
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    p.setActive(false);
  }
}
