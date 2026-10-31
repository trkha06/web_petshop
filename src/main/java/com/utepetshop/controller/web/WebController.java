package com.utepetshop.controller.web;

import com.utepetshop.dto.CheckoutRequest;
import com.utepetshop.dto.ReviewResponse;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.service.*;
import java.math.BigDecimal;
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
  private final AddressRepository addresses;
  private final CurrentUser current;
  private final StoreService store;
  private final ReviewService reviewService;
  private final StaffProfileService staffProfileService;

  public WebController(
      ProductRepository products,
      CategoryRepository categories,
      BrandRepository brands,
      BranchRepository branches,
      InventoryRepository inventories,
      CartRepository carts,
      OrderRepository orders,
      AddressRepository addresses,
      CurrentUser current,
      StoreService store,
      ReviewService reviewService,
      StaffProfileService staffProfileService) {
    this.products = products;
    this.categories = categories;
    this.brands = brands;
    this.branches = branches;
    this.inventories = inventories;
    this.carts = carts;
    this.orders = orders;
    this.addresses = addresses;
    this.current = current;
    this.store = store;
    this.reviewService = reviewService;
    this.staffProfileService = staffProfileService;
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
    List<ReviewResponse> reviews = reviewService.getProductReviews(id);
    m.addAttribute("reviews", reviews);
    m.addAttribute(
        "averageRating", reviews.stream().mapToInt(ReviewResponse::getRating).average().orElse(0));
    return "products/detail";
  }

  @GetMapping("/login")
  String login() {
    return "auth/login";
  }

  @GetMapping("/access-denied")
  @org.springframework.web.bind.annotation.ResponseStatus(
      org.springframework.http.HttpStatus.FORBIDDEN)
  String accessDenied() {
    return "error/403";
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
  String checkout(Model model, RedirectAttributes ra) {
    User user = current.required();
    Cart cart = carts.findByUserId(user.getId()).orElseGet(() -> new Cart(user));
    if (cart.getItems().isEmpty()) {
      ra.addFlashAttribute("error", "Giỏ hàng đang trống");
      return "redirect:/cart";
    }

    BigDecimal subtotal =
        cart.getItems().stream()
            .map(
                item ->
                    item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Address defaultAddress =
        addresses.findByUserIdOrderByDefaultAddressDesc(user.getId()).stream()
            .findFirst()
            .orElse(null);

    model.addAttribute("customer", user);
    model.addAttribute("cart", cart);
    model.addAttribute("subtotal", subtotal);
    model.addAttribute("defaultAddress", defaultAddress);
    model.addAttribute("defaultShippingAddress", formatAddress(defaultAddress));
    model.addAttribute("checkoutRequestKey", UUID.randomUUID().toString());
    return "checkout/index";
  }

  @PostMapping("/checkout")
  String place(
      @RequestParam PaymentMethod paymentMethod,
      @RequestParam DeliveryType deliveryType,
      @RequestParam String recipientName,
      @RequestParam String recipientEmail,
      @RequestParam String recipientPhone,
      @RequestParam(required = false) String shippingAddress,
      @RequestParam(required = false) ShippingZone shippingZone,
      @RequestParam(required = false) String customerNote,
      @RequestParam(required = false) String promotionCode,
      @RequestParam String idempotencyKey,
      RedirectAttributes ra) {
    CheckoutRequest request =
        new CheckoutRequest(
            paymentMethod,
            deliveryType,
            recipientName,
            recipientEmail,
            recipientPhone,
            shippingAddress,
            shippingZone,
            customerNote,
            promotionCode);
    try {
      CustomerOrder order = store.checkout(current.required(), request, idempotencyKey);
      if (request.paymentMethod() == PaymentMethod.ONLINE_PAYMENT) {
        return "redirect:/checkout/online-payment/" + order.getId();
      }
      ra.addFlashAttribute("success", "Đặt hàng thành công. Mã đơn #" + order.getId());
      return "redirect:/orders/" + order.getId();
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      ra.addFlashAttribute("checkoutData", request);
      return "redirect:/checkout";
    }
  }

  @GetMapping("/checkout/online-payment/{id}")
  String onlinePaymentPage(@PathVariable Long id, Model model) {
    User user = current.required();
    CustomerOrder order =
        orders
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng"));
    if (!order.getCustomer().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Bạn không có quyền truy cập đơn hàng này");
    }
    model.addAttribute("order", order);
    return "checkout/online-payment";
  }

  @PostMapping("/checkout/online-payment/{id}/confirm")
  String confirmOnlinePayment(
      @PathVariable Long id,
      @RequestParam(required = false) String otp,
      RedirectAttributes ra) {
    User user = current.required();
    CustomerOrder order =
        orders
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng"));
    if (!order.getCustomer().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Bạn không có quyền truy cập đơn hàng này");
    }
    if (otp == null || otp.trim().length() != 6) {
      ra.addFlashAttribute("error", "Mã xác thực OTP phải gồm 6 chữ số");
      return "redirect:/checkout/online-payment/" + id;
    }
    try {
      store.transition(id, OrderStatus.CONFIRMED);
      ra.addFlashAttribute(
          "success", "Thanh toán trực tuyến thành công! Đơn hàng #" + id + " đã được xác nhận.");
      return "redirect:/orders/" + id;
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/checkout/online-payment/" + id;
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
    CustomerOrder o =
        orders
            .findByIdAndCustomerId(id, current.required().getId())
            .orElseThrow(
                () ->
                    new org.springframework.security.access.AccessDeniedException(
                        "Bạn không có quyền xem đơn hàng này"));
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

  @PostMapping("/orders/{id}/confirm-received")
  String confirmReceived(@PathVariable Long id, RedirectAttributes ra) {
    try {
      store.confirmReceived(current.required(), id);
      ra.addFlashAttribute("success", "Đã xác nhận nhận hàng. Bạn có thể đánh giá từng sản phẩm trong đơn.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  @PostMapping("/orders/{id}/return")
  String requestReturn(
      @PathVariable Long id, @RequestParam String returnReason, RedirectAttributes ra) {
    try {
      store.requestReturn(current.required(), id, returnReason);
      ra.addFlashAttribute("success", "Đã gửi yêu cầu hoàn đơn. UTEPet sẽ liên hệ để xử lý.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  @GetMapping("/manager")
  String manager() {
    return "redirect:/manager/dashboard";
  }

  @GetMapping("/staff/orders")
  String staff(Model m) {
    User user = current.required();
    if (staffProfileService.hasSystemManagementAccess(user)) {
      m.addAttribute("orders", orders.findAllByOrderByCreatedAtDesc());
      m.addAttribute("staffBranch", null);
    } else {
      Branch branch = staffProfileService.getAssignedBranch(user);
      m.addAttribute("orders", orders.findByBranchIdOrderByCreatedAtDesc(branch.getId()));
      m.addAttribute("staffBranch", branch);
    }
    m.addAttribute("statuses", OrderStatus.values());
    return "staff/orders";
  }

  @PostMapping("/staff/orders/{id}")
  String updateOrder(
      @PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
    try {
      CustomerOrder order =
          orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
      staffProfileService.requireBranchAccess(current.required(), order.getBranch().getId());
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

  private String formatAddress(Address address) {
    if (address == null) return "";
    return String.join(
        ", ",
        Arrays.stream(
                new String[] {
                  address.getLine1(), address.getWard(), address.getDistrict(), address.getCity()
                })
            .filter(value -> value != null && !value.isBlank())
            .toList());
  }
}
