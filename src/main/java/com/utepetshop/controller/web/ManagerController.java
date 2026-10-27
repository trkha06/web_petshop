package com.utepetshop.controller.web;

import com.utepetshop.dto.ProductRequest;
import com.utepetshop.entity.PetType;
import com.utepetshop.entity.Product;
import com.utepetshop.repository.BrandRepository;
import com.utepetshop.repository.CategoryRepository;
import com.utepetshop.repository.InventoryRepository;
import com.utepetshop.repository.ProductRepository;
import com.utepetshop.service.CatalogService;
import com.utepetshop.service.InventoryService;
import java.math.BigDecimal;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager")
public class ManagerController {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final BrandRepository brands;
  private final InventoryRepository inventories;
  private final CatalogService catalog;
  private final InventoryService inventory;

  public ManagerController(
      ProductRepository products,
      CategoryRepository categories,
      BrandRepository brands,
      InventoryRepository inventories,
      CatalogService catalog,
      InventoryService inventory) {
    this.products = products;
    this.categories = categories;
    this.brands = brands;
    this.inventories = inventories;
    this.catalog = catalog;
    this.inventory = inventory;
  }

  @GetMapping("/products")
  String products(Model model) {
    model.addAttribute(
        "products", products.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
    return "manager/products/list";
  }

  @GetMapping("/products/new")
  String form(Model model) {
    prepare(model, null);
    return "manager/products/form";
  }

  @GetMapping("/products/{id}/edit")
  String edit(@PathVariable Long id, Model model) {
    Product product =
        products
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    prepare(model, product);
    return "manager/products/form";
  }

  @PostMapping("/products")
  String save(
      @RequestParam(required = false) Long id,
      @RequestParam String sku,
      @RequestParam String name,
      @RequestParam(required = false) String description,
      @RequestParam BigDecimal price,
      @RequestParam Long categoryId,
      @RequestParam Long brandId,
      @RequestParam PetType petType,
      @RequestParam(required = false) String imageUrl,
      RedirectAttributes redirect) {
    try {
      Product saved =
          catalog.save(
              id,
              new ProductRequest(
                  sku, name, description, price, categoryId, brandId, petType, imageUrl));
      redirect.addFlashAttribute(
          "success",
          id == null
              ? "Đã thêm sản phẩm. Hãy cập nhật tồn kho để bắt đầu bán."
              : "Đã cập nhật sản phẩm " + saved.getName());
    } catch (IllegalArgumentException exception) {
      redirect.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/manager/products";
  }

  @PostMapping("/products/{id}/deactivate")
  String deactivate(@PathVariable Long id, RedirectAttributes redirect) {
    catalog.deactivate(id);
    redirect.addFlashAttribute("success", "Đã ngừng bán sản phẩm");
    return "redirect:/manager/products";
  }

  @PostMapping("/products/{id}/status")
  String status(
      @PathVariable Long id,
      @RequestParam boolean active,
      RedirectAttributes redirect) {
    catalog.setActive(id, active);
    redirect.addFlashAttribute(
        "success", active ? "Sản phẩm đã được bán lại" : "Đã ngừng bán sản phẩm");
    return "redirect:/manager/products";
  }

  @GetMapping("/inventory")
  String inventory(Model model) {
    model.addAttribute("inventories", inventories.findAll());
    return "manager/inventory/list";
  }

  @PostMapping("/inventory/{id}/adjust")
  String adjust(
      @PathVariable Long id,
      @RequestParam int delta,
      @RequestParam String note,
      RedirectAttributes redirect) {
    try {
      inventory.adjust(id, delta, note);
      redirect.addFlashAttribute("success", "Đã điều chỉnh tồn kho");
    } catch (IllegalArgumentException exception) {
      redirect.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/manager/inventory";
  }

  private void prepare(Model model, Product product) {
    model.addAttribute("product", product);
    model.addAttribute("categories", categories.findAll());
    model.addAttribute("brands", brands.findAll());
    model.addAttribute("petTypes", PetType.values());
  }
}
