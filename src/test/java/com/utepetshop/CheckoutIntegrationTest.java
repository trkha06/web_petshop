package com.utepetshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.utepetshop.dto.CheckoutRequest;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.service.StoreService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private StoreService store;
  @Autowired private UserRepository users;
  @Autowired private ProductRepository products;
  @Autowired private InventoryRepository inventories;
  @Autowired private PromotionRepository promotions;
  @Autowired private CartRepository carts;

  @Test
  void checkoutSavesRecipientAndAppliesPromotion() {
    User customer = new User();
    customer.setEmail("checkout-test@utepet.vn");
    customer.setPassword("Password@123");
    customer.setFullName("Khách kiểm thử");
    customer = users.save(customer);

    Product product = products.findBySkuIgnoreCase("DOG-001").orElseThrow();
    Inventory inventory = inventories.findByProductId(product.getId()).getFirst();
    int stockBefore = inventory.getQuantity();
    int promotionUsesBefore = promotions.findByCode("CHAOMUNG").orElseThrow().getUsedCount();

    store.addToCart(customer, product.getId(), inventory.getBranch().getId(), 1);

    CheckoutRequest request =
        new CheckoutRequest(
            PaymentMethod.COD,
            DeliveryType.DELIVERY,
            "Nguyễn Văn An",
            "an@example.com",
            "0901234567",
            "01 Võ Văn Ngân, Thủ Đức, TP.HCM",
            ShippingZone.INNER_CITY,
            "Giao giờ hành chính",
            "CHAOMUNG");

    CustomerOrder order = store.checkout(customer, request);

    assertThat(order.getRecipientName()).isEqualTo("Nguyễn Văn An");
    assertThat(order.getRecipientEmail()).isEqualTo("an@example.com");
    assertThat(order.getRecipientPhone()).isEqualTo("0901234567");
    assertThat(order.getShippingAddress()).contains("Võ Văn Ngân");
    assertThat(order.getCustomerNote()).isEqualTo("Giao giờ hành chính");
    assertThat(order.getPromotionCode()).isEqualTo("CHAOMUNG");
    assertThat(order.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("245000"));
    assertThat(order.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("24500"));
    assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("220500"));
    assertThat(inventories.findById(inventory.getId()).orElseThrow().getQuantity())
        .isEqualTo(stockBefore - 1);
    assertThat(promotions.findByCode("CHAOMUNG").orElseThrow().getUsedCount())
        .isEqualTo(promotionUsesBefore + 1);
    assertThat(carts.findByUserId(customer.getId()).orElseThrow().getItems()).isEmpty();
  }

  @Test
  void customerCanApplyPromotionButCannotManagePromotions() throws Exception {
    mvc.perform(
            post("/api/promotions/apply")
                .with(user("customer@utepet.vn").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CHAOMUNG\",\"orderAmount\":245000}"))
        .andExpect(status().isOk());

    mvc.perform(
            post("/api/promotions")
                .with(user("customer@utepet.vn").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void checkoutPageRendersContactAddressAndPromotionFields() throws Exception {
    User customer = new User();
    customer.setEmail("checkout-page@utepet.vn");
    customer.setPassword("Password@123");
    customer.setFullName("Khách giao diện");
    customer = users.save(customer);

    Product product = products.findBySkuIgnoreCase("DOG-001").orElseThrow();
    Inventory inventory = inventories.findByProductId(product.getId()).getFirst();
    store.addToCart(customer, product.getId(), inventory.getBranch().getId(), 1);

    mvc.perform(get("/checkout").with(user(customer.getEmail()).roles("CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(view().name("checkout/index"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"recipientName\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"recipientPhone\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"shippingAddress\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"promotionCode\"")));
  }
}
