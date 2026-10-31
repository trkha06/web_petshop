package com.utepetshop.service;

import com.utepetshop.dto.CheckoutRequest;
import com.utepetshop.dto.PromotionDiscountResponse;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {
  private final CartRepository carts;
  private final CartItemRepository cartItems;
  private final ProductRepository products;
  private final BranchRepository branches;
  private final InventoryRepository inventories;
  private final OrderRepository orders;
  private final InventoryTransactionRepository transactions;
  private final PromotionService promotions;
  private final org.springframework.context.ApplicationEventPublisher eventPublisher;

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9][0-9 .()-]{8,19}$");

  public StoreService(
      CartRepository carts,
      CartItemRepository cartItems,
      ProductRepository products,
      BranchRepository branches,
      InventoryRepository inventories,
      OrderRepository orders,
      InventoryTransactionRepository transactions,
      PromotionService promotions,
      org.springframework.context.ApplicationEventPublisher eventPublisher) {
    this.carts = carts;
    this.cartItems = cartItems;
    this.products = products;
    this.branches = branches;
    this.inventories = inventories;
    this.orders = orders;
    this.transactions = transactions;
    this.promotions = promotions;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Cart addToCart(User user, Long productId, Long branchId, int quantity) {
    if (quantity < 1) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
    Product product =
        products
            .findById(productId)
            .filter(Product::isActive)
            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không khả dụng"));
    Branch branch =
        branches
            .findById(branchId)
            .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại"));
    Inventory stock =
        inventories
            .findByProductIdAndBranchId(productId, branchId)
            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chưa có tại chi nhánh này"));
    Cart cart = carts.findByUserId(user.getId()).orElseGet(() -> carts.save(new Cart(user)));
    CartItem item =
        cartItems
            .findByCartIdAndProductIdAndBranchId(cart.getId(), productId, branchId)
            .orElse(null);
    int requested = quantity + (item == null ? 0 : item.getQuantity());
    if (requested > stock.getQuantity())
      throw new IllegalArgumentException("Số lượng vượt tồn kho hiện có");
    if (item == null) cart.getItems().add(new CartItem(cart, product, branch, quantity));
    else item.setQuantity(requested);
    return carts.save(cart);
  }

  @Transactional
  public CustomerOrder checkout(User user, CheckoutRequest request) {
    return checkout(user, request, UUID.randomUUID().toString());
  }

  @Transactional
  public CustomerOrder checkout(User user, CheckoutRequest request, String idempotencyKey) {
    validateCheckout(request);
    String checkoutRequestKey = validateIdempotencyKey(idempotencyKey);
    CustomerOrder existingOrder = findExistingOrder(checkoutRequestKey, user);
    if (existingOrder != null) {
      return existingOrder;
    }

    Cart cart =
        carts
            .findByUserIdForCheckoutLock(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng đang trống"));
    existingOrder = findExistingOrder(checkoutRequestKey, user);
    if (existingOrder != null) {
      return existingOrder;
    }
    if (cart.getItems().isEmpty()) throw new IllegalArgumentException("Giỏ hàng đang trống");
    Long branchId = cart.getItems().getFirst().getBranch().getId();
    if (cart.getItems().stream().anyMatch(i -> !i.getBranch().getId().equals(branchId)))
      throw new IllegalArgumentException("Mỗi đơn hàng chỉ xử lý tại một chi nhánh");

    BigDecimal subtotal =
        cart.getItems().stream()
            .map(
                item ->
                    item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    PromotionDiscountResponse discount = null;
    String promotionCode = normalize(request.promotionCode());
    if (promotionCode != null) {
      discount = promotions.reservePromotion(promotionCode, subtotal);
      if (!discount.isValid()) throw new IllegalArgumentException(discount.getMessage());
    }

    Branch branch = branches.getReferenceById(branchId);
    CustomerOrder order = new CustomerOrder();
    order.setCustomer(user);
    order.setBranch(branch);
    order.setCheckoutRequestKey(checkoutRequestKey);
    order.setPaymentMethod(request.paymentMethod());
    order.setDeliveryType(request.deliveryType());
    order.setShippingZone(resolveShippingZone(request));
    order.setShippingCarrier(request.deliveryType() == DeliveryType.DELIVERY ? "XPS" : null);
    order.setTrackingCode(request.deliveryType() == DeliveryType.DELIVERY ? createTrackingCode() : null);
    order.setRecipientName(request.recipientName().trim());
    order.setRecipientEmail(request.recipientEmail().trim().toLowerCase());
    order.setRecipientPhone(request.recipientPhone().trim());
    order.setShippingAddress(resolveAddress(request, branch));
    order.setCustomerNote(normalize(request.customerNote()));
    order.setPromotionCode(discount == null ? null : discount.getCode());
    order.setSubtotalAmount(subtotal);
    order.setDiscountAmount(discount == null ? BigDecimal.ZERO : discount.getDiscountAmount());
    order.setTotalAmount(discount == null ? subtotal : discount.getFinalAmount());

    for (CartItem ci : cart.getItems()) {
      Inventory inv =
          inventories
              .lockByProductAndBranch(ci.getProduct().getId(), branchId)
              .orElseThrow(() -> new IllegalStateException("Không tìm thấy tồn kho"));
      if (!ci.getProduct().isActive() || inv.getQuantity() < ci.getQuantity())
        throw new IllegalArgumentException("Không đủ tồn kho cho " + ci.getProduct().getName());
      inv.setQuantity(inv.getQuantity() - ci.getQuantity());
      transactions.save(
          new InventoryTransaction(
              inv, InventoryTransactionType.SALE, -ci.getQuantity(), "Order đang tạo"));
      OrderItem oi = new OrderItem();
      oi.setOrder(order);
      oi.setProduct(ci.getProduct());
      oi.setProductName(ci.getProduct().getName());
      oi.setUnitPrice(ci.getProduct().getPrice());
      oi.setQuantity(ci.getQuantity());
      order.getItems().add(oi);
    }

    CustomerOrder saved = orders.saveAndFlush(order);
    cart.getItems().clear();
    carts.save(cart);
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new com.utepetshop.service.event.OrderStatusChangedEvent(
              this, saved, "Đơn hàng mới #" + saved.getId() + " đã được tạo"));
    }
    return saved;
  }

  private CustomerOrder findExistingOrder(String checkoutRequestKey, User user) {
    CustomerOrder order = orders.findByCheckoutRequestKey(checkoutRequestKey).orElse(null);
    if (order != null && !order.getCustomer().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Mã yêu cầu thanh toán không hợp lệ");
    }
    return order;
  }

  private String validateIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("Thiếu mã yêu cầu thanh toán");
    }
    String value = idempotencyKey.trim();
    if (value.length() < 16 || value.length() > 80) {
      throw new IllegalArgumentException("Mã yêu cầu thanh toán không hợp lệ");
    }
    return value;
  }

  private void validateCheckout(CheckoutRequest request) {
    if (request == null) throw new IllegalArgumentException("Thông tin đặt hàng không hợp lệ");
    if (request.paymentMethod() == null || request.deliveryType() == null)
      throw new IllegalArgumentException("Vui lòng chọn hình thức nhận hàng và thanh toán");

    String name = normalize(request.recipientName());
    if (name == null || name.length() > 100)
      throw new IllegalArgumentException("Vui lòng nhập họ tên người nhận");

    String email = normalize(request.recipientEmail());
    if (email == null || email.length() > 150 || !EMAIL_PATTERN.matcher(email).matches())
      throw new IllegalArgumentException("Email người nhận không hợp lệ");

    String phone = normalize(request.recipientPhone());
    String phoneDigits = phone == null ? "" : phone.replaceAll("\\D", "");
    if (phone == null
        || !PHONE_PATTERN.matcher(phone).matches()
        || phoneDigits.length() < 9
        || phoneDigits.length() > 12)
      throw new IllegalArgumentException("Số điện thoại người nhận không hợp lệ");

    if (request.deliveryType() == DeliveryType.DELIVERY
        && normalize(request.shippingAddress()) == null)
      throw new IllegalArgumentException("Vui lòng nhập địa chỉ giao hàng");

    if (request.deliveryType() == DeliveryType.PICKUP_AT_STORE && request.shippingZone() != null)
      throw new IllegalArgumentException("Nhận tại cửa hàng không cần chọn khu vực giao hàng");

    if (request.shippingAddress() != null && request.shippingAddress().trim().length() > 500)
      throw new IllegalArgumentException("Địa chỉ giao hàng quá dài");

    if (request.customerNote() != null && request.customerNote().trim().length() > 500)
      throw new IllegalArgumentException("Ghi chú đơn hàng quá dài");
  }

  private String resolveAddress(CheckoutRequest request, Branch branch) {
    String address = normalize(request.shippingAddress());
    if (request.deliveryType() == DeliveryType.DELIVERY) return address;
    return "Nhận tại " + branch.getName() + " - " + branch.getAddress();
  }

  private ShippingZone resolveShippingZone(CheckoutRequest request) {
    if (request.deliveryType() != DeliveryType.DELIVERY) return null;
    if (request.shippingZone() != null) return request.shippingZone();
    String address = normalize(request.shippingAddress());
    if (address == null) return ShippingZone.OUTER_CITY;
    String normalizedAddress = address.toLowerCase(Locale.ROOT);
    return normalizedAddress.contains("hồ chí minh")
            || normalizedAddress.contains("ho chi minh")
            || normalizedAddress.contains("tp.hcm")
            || normalizedAddress.contains("tphcm")
        ? ShippingZone.INNER_CITY
        : ShippingZone.OUTER_CITY;
  }

  private String createTrackingCode() {
    return "XPS" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  @Transactional
  public void updateCartItem(User user, Long itemId, int quantity) {
    if (quantity < 1) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
    CartItem item =
        cartItems
            .findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ"));
    if (!item.getCart().getUser().getId().equals(user.getId()))
      throw new IllegalArgumentException("Không có quyền sửa giỏ hàng này");
    Inventory inv =
        inventories
            .findByProductIdAndBranchId(item.getProduct().getId(), item.getBranch().getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho"));
    if (quantity > inv.getQuantity())
      throw new IllegalArgumentException("Số lượng vượt tồn kho hiện có");
    item.setQuantity(quantity);
  }

  @Transactional
  public void removeCartItem(User user, Long itemId) {
    CartItem item =
        cartItems
            .findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ"));
    if (!item.getCart().getUser().getId().equals(user.getId()))
      throw new IllegalArgumentException("Không có quyền sửa giỏ hàng này");
    cartItems.delete(item);
  }

  @Transactional
  public void cancelByCustomer(User user, Long orderId) {
    CustomerOrder order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    if (!order.getCustomer().getId().equals(user.getId()))
      throw new IllegalArgumentException("Không có quyền hủy đơn này");
    if (order.getStatus() != OrderStatus.PENDING)
      throw new IllegalArgumentException("Chỉ được hủy đơn ở trạng thái PENDING");
    restockAndCancel(order);
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new com.utepetshop.service.event.OrderStatusChangedEvent(
              this, order, "Đơn hàng #" + order.getId() + " đã bị hủy"));
    }
  }

  @Transactional
  public void confirmReceived(User user, Long orderId) {
    CustomerOrder order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    if (!order.getCustomer().getId().equals(user.getId()))
      throw new IllegalArgumentException("Bạn không có quyền xác nhận đơn hàng này");
    if (order.getStatus() != OrderStatus.SHIPPING)
      throw new IllegalArgumentException("Chỉ xác nhận khi đơn đang giao hàng");
    order.setStatus(OrderStatus.COMPLETED);
    CustomerOrder updated = orders.save(order);
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new com.utepetshop.service.event.OrderStatusChangedEvent(
              this, updated, "Khách hàng đã xác nhận nhận đơn #" + updated.getId()));
    }
  }

  @Transactional
  public void requestReturn(User user, Long orderId, String reason) {
    CustomerOrder order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    if (!order.getCustomer().getId().equals(user.getId()))
      throw new IllegalArgumentException("Bạn không có quyền yêu cầu hoàn đơn này");
    if (order.getStatus() != OrderStatus.COMPLETED)
      throw new IllegalArgumentException("Chỉ có thể yêu cầu hoàn đơn sau khi đã nhận hàng");
    String returnReason = normalize(reason);
    if (returnReason == null || returnReason.length() < 10)
      throw new IllegalArgumentException("Vui lòng nhập lý do hoàn đơn ít nhất 10 ký tự");
    if (returnReason.length() > 500)
      throw new IllegalArgumentException("Lý do hoàn đơn không được quá 500 ký tự");
    order.setReturnReason(returnReason);
    order.setReturnRequestedAt(java.time.LocalDateTime.now());
    order.setStatus(OrderStatus.RETURN_REQUESTED);
    CustomerOrder updated = orders.save(order);
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new com.utepetshop.service.event.OrderStatusChangedEvent(
              this, updated, "Khách hàng yêu cầu hoàn đơn #" + updated.getId()));
    }
  }

  private void restockAndCancel(CustomerOrder order) {
    restockOrder(order, InventoryTransactionType.CANCEL_ORDER, "Hủy order #" + order.getId());
    order.setStatus(OrderStatus.CANCELLED);
  }

  private void restockOrder(CustomerOrder order, InventoryTransactionType type, String note) {
    for (OrderItem item : order.getItems()) {
      Inventory inv =
          inventories
              .lockByProductAndBranch(item.getProduct().getId(), order.getBranch().getId())
              .orElseThrow(() -> new IllegalStateException("Không tìm thấy tồn kho"));
      inv.setQuantity(inv.getQuantity() + item.getQuantity());
      transactions.save(
          new InventoryTransaction(
              inv,
              type,
              item.getQuantity(),
              note));
    }
  }

  @Transactional
  public void transition(Long orderId, OrderStatus next) {
    CustomerOrder order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    Map<OrderStatus, Set<OrderStatus>> allowed =
        Map.of(
            OrderStatus.PENDING,
            Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,
            Set.of(OrderStatus.PACKING, OrderStatus.CANCELLED),
            OrderStatus.PACKING,
            Set.of(OrderStatus.SHIPPING),
            OrderStatus.SHIPPING,
            Set.of(),
            OrderStatus.RETURN_REQUESTED,
            Set.of(OrderStatus.RETURNED));
    if (!allowed.getOrDefault(order.getStatus(), Set.of()).contains(next))
      throw new IllegalArgumentException("Không thể chuyển trạng thái đơn hàng theo cách này");
    if (next == OrderStatus.CANCELLED) {
      restockAndCancel(order);
    } else if (next == OrderStatus.RETURNED) {
      restockOrder(order, InventoryTransactionType.RETURN_ORDER, "Hoàn order #" + order.getId());
      order.setStatus(OrderStatus.RETURNED);
    } else {
      order.setStatus(next);
    }
    CustomerOrder updated = orders.save(order);
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new com.utepetshop.service.event.OrderStatusChangedEvent(
              this, updated, "Đơn hàng #" + updated.getId() + " chuyển sang " + next));
    }
  }
}
