package com.utepetshop.controller.web;

import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.service.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final BrandRepository brands;
  private final BranchRepository branches;
  private final InventoryRepository inventories;
  private final CartRepository carts;
  private final OrderRepository orders;
  private final CurrentUser current;
  private final StoreService store;

  public WebController(
      ProductRepository products,
      CategoryRepository categories,
      BrandRepository brands,
      BranchRepository branches,
      InventoryRepository inventories,
      CartRepository carts,
      OrderRepository orders,
      CurrentUser current,
      StoreService store) {
    this.products = products;
    this.categories = categories;
    this.brands = brands;
    this.branches = branches;
    this.inventories = inventories;
    this.carts = carts;
    this.orders = orders;
    this.current = current;
    this.store = store;
  }

  @GetMapping({"/", "/products"})
  String list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long category,
      @RequestParam(required = false) Long brand,
      @RequestParam(required = false) PetType petType,
      @RequestParam(required = false) java.math.BigDecimal minPrice,
      @RequestParam(required = false) java.math.BigDecimal maxPrice,
      @RequestParam(required = false) Long branch,
      @RequestParam(defaultValue = "false") boolean inStock,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "newest") String sort,
      Model m) {
    Sort s =
        "priceAsc".equals(sort)
            ? Sort.by("price").ascending()
            : "priceDesc".equals(sort)
                ? Sort.by("price").descending()
                : Sort.by("createdAt").descending();
    Page<Product> result =
        products.search(
            blank(q),
            category,
            brand,
            petType,
            minPrice,
            maxPrice,
            branch,
            inStock,
            PageRequest.of(Math.max(page, 0), 8, s));
    m.addAttribute("products", result);
    m.addAttribute("categories", categories.findAll());
    m.addAttribute("brands", brands.findAll());
    m.addAttribute("branches", branches.findAll());
    m.addAttribute("q", q);
    m.addAttribute("selectedCategory", category);
    m.addAttribute("selectedBrand", brand);
    m.addAttribute("selectedPetType", petType);
    m.addAttribute("sort", sort);
    return "products/list";
  }

  @GetMapping("/products/{id}")
  String detail(@PathVariable Long id, Model m) {
    Product p =
        products
            .findById(id)
            .filter(Product::isActive)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sản phẩm"));
    m.addAttribute("product", p);
    m.addAttribute("stocks", inventories.findByProductId(id));
    return "products/detail";
  }

  @GetMapping("/login")
  String login() {
    return "auth/login";
  }

  @GetMapping("/cart")
  String cart(Model m) {
    m.addAttribute(
        "cart",
        carts
            .findByUserId(current.required().getId())
            .orElseGet(() -> new Cart(current.required())));
    return "cart/index";
  }

  @PostMapping("/cart/items")
  String add(
      @RequestParam Long productId,
      @RequestParam Long branchId,
      @RequestParam int quantity,
      RedirectAttributes ra) {
    try {
      store.addToCart(current.required(), productId, branchId, quantity);
      ra.addFlashAttribute("success", "Đã thêm vào giỏ hàng");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/products/" + productId;
  }

  @PostMapping("/buy-now")
  String buyNow(
      @RequestParam Long productId,
      @RequestParam Long branchId,
      @RequestParam int quantity,
      RedirectAttributes ra) {
    try {
      store.addToCart(current.required(), productId, branchId, quantity);
      return "redirect:/checkout";
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/products/" + productId;
    }
  }

  @PostMapping("/cart/items/{id}/quantity")
  String updateCart(@PathVariable Long id, @RequestParam int quantity, RedirectAttributes ra) {
    try {
      store.updateCartItem(current.required(), id, quantity);
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/cart";
  }

  @PostMapping("/cart/items/{id}/remove")
  String removeCart(@PathVariable Long id, RedirectAttributes ra) {
    try {
      store.removeCartItem(current.required(), id);
      ra.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/cart";
  }

  @GetMapping("/checkout")
  String checkout() {
    return "checkout/index";
  }

  @PostMapping("/checkout")
  String place(
      @RequestParam PaymentMethod paymentMethod,
      @RequestParam DeliveryType deliveryType,
      RedirectAttributes ra) {
    try {
      CustomerOrder order = store.checkout(current.required(), paymentMethod, deliveryType);
      ra.addFlashAttribute("success", "Đặt hàng thành công. Mã đơn #" + order.getId());
      return "redirect:/orders/" + order.getId();
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/cart";
    }
  }

  @GetMapping("/orders")
  String orderList(Model m) {
    m.addAttribute(
        "orders", orders.findByCustomerIdOrderByCreatedAtDesc(current.required().getId()));
    return "orders/list";
  }

  @GetMapping("/orders/{id}")
  String orderDetail(@PathVariable Long id, Model m) {
    CustomerOrder o = orders.findById(id).orElseThrow();
    if (!o.getCustomer().getId().equals(current.required().getId()))
      throw new org.springframework.security.access.AccessDeniedException(
          "Không có quyền xem đơn này");
    m.addAttribute("order", o);
    return "orders/detail";
  }

  @PostMapping("/orders/{id}/cancel")
  String cancel(@PathVariable Long id, RedirectAttributes ra) {
    try {
      store.cancelByCustomer(current.required(), id);
      ra.addFlashAttribute("success", "Đã hủy đơn và hoàn lại tồn kho");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  @GetMapping("/manager")
  String manager(Model m) {
    m.addAttribute("products", products.findAll());
    m.addAttribute("branches", branches.findAll());
    return "manager/dashboard";
  }

  @GetMapping("/staff/orders")
  String staff(Model m) {
    m.addAttribute("orders", orders.findAllByOrderByCreatedAtDesc());
    m.addAttribute("statuses", OrderStatus.values());
    return "staff/orders";
  }

  @PostMapping("/staff/orders/{id}")
  String updateOrder(
      @PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
    try {
      store.transition(id, status);
      ra.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/staff/orders";
  }

  private String blank(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
