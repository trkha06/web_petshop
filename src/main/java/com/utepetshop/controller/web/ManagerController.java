package com.utepetshop.controller.web;

import com.utepetshop.dto.ProductRequest;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.service.*;
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
      ProductRepository p,
      CategoryRepository c,
      BrandRepository b,
      InventoryRepository i,
      CatalogService cs,
      InventoryService is) {
    products = p;
    categories = c;
    brands = b;
    inventories = i;
    catalog = cs;
    inventory = is;
  }

  @GetMapping("/products")
  String products(Model m) {
    m.addAttribute("products", products.findAll());
    return "manager/products/list";
  }

  @GetMapping("/products/new")
  String form(Model m) {
    prepare(m, null);
    return "manager/products/form";
  }

  @GetMapping("/products/{id}/edit")
  String edit(@PathVariable Long id, Model m) {
    Product p = products.findById(id).orElseThrow();
    prepare(m, p);
    return "manager/products/form";
  }

  @PostMapping("/products")
  String save(
      @RequestParam(required = false) Long id,
      @RequestParam String sku,
      @RequestParam String name,
      @RequestParam(required = false) String description,
      @RequestParam java.math.BigDecimal price,
      @RequestParam Long categoryId,
      @RequestParam Long brandId,
      @RequestParam PetType petType,
      @RequestParam(required = false) String imageUrl,
      RedirectAttributes r) {
    try {
      catalog.save(
          id,
          new ProductRequest(
              sku, name, description, price, categoryId, brandId, petType, imageUrl));
      r.addFlashAttribute("success", "Đã lưu sản phẩm");
    } catch (Exception e) {
      r.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/products";
  }

  @PostMapping("/products/{id}/deactivate")
  String deactivate(@PathVariable Long id) {
    catalog.deactivate(id);
    return "redirect:/manager/products";
  }

  @GetMapping("/inventory")
  String inventory(Model m) {
    m.addAttribute("inventories", inventories.findAll());
    return "manager/inventory/list";
  }

  @PostMapping("/inventory/{id}/adjust")
  String adjust(
      @PathVariable Long id,
      @RequestParam int delta,
      @RequestParam String note,
      RedirectAttributes r) {
    try {
      inventory.adjust(id, delta, note);
      r.addFlashAttribute("success", "Đã điều chỉnh tồn kho");
    } catch (IllegalArgumentException e) {
      r.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/inventory";
  }

  private void prepare(Model m, Product p) {
    m.addAttribute("product", p);
    m.addAttribute("categories", categories.findAll());
    m.addAttribute("brands", brands.findAll());
    m.addAttribute("petTypes", PetType.values());
  }
}
