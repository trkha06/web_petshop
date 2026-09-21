package com.utepetshop.service;

import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import java.math.BigDecimal;
import java.util.*;
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
  private final org.springframework.context.ApplicationEventPublisher eventPublisher;

  public StoreService(
      CartRepository carts,
      CartItemRepository cartItems,
      ProductRepository products,
      BranchRepository branches,
      InventoryRepository inventories,
      OrderRepository orders,
      InventoryTransactionRepository transactions,
      org.springframework.context.ApplicationEventPublisher eventPublisher) {
    this.carts = carts;
    this.cartItems = cartItems;
    this.products = products;
    this.branches = branches;
    this.inventories = inventories;
    this.orders = orders;
    this.transactions = transactions;
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
  public CustomerOrder checkout(User user, PaymentMethod method, DeliveryType delivery) {
    Cart cart =
        carts
            .findByUserId(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng đang trống"));
    if (cart.getItems().isEmpty()) throw new IllegalArgumentException("Giỏ hàng đang trống");
    Long branchId = cart.getItems().getFirst().getBranch().getId();
    if (cart.getItems().stream().anyMatch(i -> !i.getBranch().getId().equals(branchId)))
      throw new IllegalArgumentException("Mỗi đơn hàng chỉ xử lý tại một chi nhánh");
    CustomerOrder order = new CustomerOrder();
    order.setCustomer(user);
    order.setBranch(branches.getReferenceById(branchId));
    order.setPaymentMethod(method);
    order.setDeliveryType(delivery);
    BigDecimal total = BigDecimal.ZERO;
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
      total = total.add(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
    }
    order.setTotalAmount(total);
    CustomerOrder saved = orders.save(order);
    cart.getItems().clear();
    carts.save(cart);
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new com.utepetshop.service.event.OrderStatusChangedEvent(
              this, saved, "Đơn hàng mới #" + saved.getId() + " đã được tạo"));
    }
    return saved;
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

  private void restockAndCancel(CustomerOrder order) {
    for (OrderItem item : order.getItems()) {
      Inventory inv =
          inventories
              .lockByProductAndBranch(item.getProduct().getId(), order.getBranch().getId())
              .orElseThrow(() -> new IllegalStateException("Không tìm thấy tồn kho"));
      inv.setQuantity(inv.getQuantity() + item.getQuantity());
      transactions.save(
          new InventoryTransaction(
              inv,
              InventoryTransactionType.CANCEL_ORDER,
              item.getQuantity(),
              "Hủy order #" + order.getId()));
    }
    order.setStatus(OrderStatus.CANCELLED);
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
            Set.of(OrderStatus.COMPLETED));
    if (!allowed.getOrDefault(order.getStatus(), Set.of()).contains(next))
      throw new IllegalArgumentException("Không thể chuyển trạng thái đơn hàng theo cách này");
    order.setStatus(next);
    CustomerOrder updated = orders.save(order);
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new com.utepetshop.service.event.OrderStatusChangedEvent(
              this, updated, "Đơn hàng #" + updated.getId() + " chuyển sang " + next));
    }
  }
}
