package com.utepetshop.service;

import com.utepetshop.dto.ProductRequest;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final BrandRepository brands;
  private final BranchRepository branches;
  private final InventoryRepository inventories;

  public CatalogService(
      ProductRepository products,
      CategoryRepository categories,
      BrandRepository brands,
      BranchRepository branches,
      InventoryRepository inventories) {
    this.products = products;
    this.categories = categories;
    this.brands = brands;
    this.branches = branches;
    this.inventories = inventories;
  }

  @Transactional
  public Product save(Long id, ProductRequest request) {
    String sku = required(request.sku(), "SKU").toUpperCase();
    String name = required(request.name(), "Tên sản phẩm");
    if (request.price() == null || request.price().signum() <= 0) {
      throw new IllegalArgumentException("Giá sản phẩm phải lớn hơn 0");
    }

    products
        .findBySkuIgnoreCase(sku)
        .filter(existing -> id == null || !existing.getId().equals(id))
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException("SKU đã được sử dụng");
            });

    boolean creating = id == null;
    Product product =
        id == null
            ? new Product()
            : products
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    product.setSku(sku);
    product.setName(name);
    product.setDescription(trimToNull(request.description()));
    product.setPrice(request.price());
    product.setCategory(
        categories
            .findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại")));
    product.setBrand(
        brands
            .findById(request.brandId())
            .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại")));
    if (request.petType() == null) {
      throw new IllegalArgumentException("Vui lòng chọn loại thú cưng");
    }
    product.setPetType(request.petType());
    product.setImageUrl(trimToNull(request.imageUrl()));
    product.setUpdatedAt(LocalDateTime.now());
    product = products.save(product);

    if (creating) {
      for (Branch branch : branches.findByActiveTrueOrderByNameAsc()) {
        inventories.save(new Inventory(product, branch, 0));
      }
    }
    return product;
  }

  @Transactional
  public void deactivate(Long id) {
    setActive(id, false);
  }

  @Transactional
  public void setActive(Long id, boolean active) {
    Product product =
        products
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    product.setActive(active);
    product.setUpdatedAt(LocalDateTime.now());
  }

  private String required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " không được để trống");
    }
    return value.trim();
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
