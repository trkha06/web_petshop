package com.utepetshop;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.utepetshop.entity.Branch;
import com.utepetshop.entity.CustomerOrder;
import com.utepetshop.entity.OrderItem;
import com.utepetshop.entity.Product;
import com.utepetshop.entity.User;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.OrderRepository;
import com.utepetshop.repository.ProductRepository;
import com.utepetshop.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebPageAndRoleSmokeTest {
  @Autowired private MockMvc mvc;
  @Autowired private OrderRepository orders;
  @Autowired private UserRepository users;
  @Autowired private BranchRepository branches;
  @Autowired private ProductRepository products;

  private Long customerOrderId;

  @BeforeEach
  void createOrderForDetailPage() {
    User customer = users.findByEmail("customer@utepet.vn").orElseThrow();
    Branch branch = branches.findAll().getFirst();
    Product product = products.findAll().getFirst();
    CustomerOrder order = new CustomerOrder();
    order.setCustomer(customer);
    order.setBranch(branch);
    order.setTotalAmount(product.getPrice());
    OrderItem item = new OrderItem();
    item.setOrder(order);
    item.setProduct(product);
    item.setProductName(product.getName());
    item.setUnitPrice(product.getPrice());
    item.setQuantity(1);
    order.getItems().add(item);
    customerOrderId = orders.save(order).getId();
  }

  @Test
  void anonymousIsRedirectedToLoginForBookingAndDeniedForOperations() throws Exception {
    mvc.perform(get("/bookings/create")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/staff/bookings")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/manager/dashboard")).andExpect(status().is3xxRedirection());
  }

  @Test
  void customerRendersBookingAndOwnOrderButCannotOperate() throws Exception {
    mvc.perform(get("/bookings/create?serviceId=1").with(user("customer@utepet.vn").roles("CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(view().name("bookings/create"));
    mvc.perform(get("/orders/{id}", customerOrderId).with(user("customer@utepet.vn").roles("CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(view().name("orders/detail"));
    mvc.perform(get("/staff/bookings").with(user("customer@utepet.vn").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/manager/dashboard").with(user("customer@utepet.vn").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void staffCanRenderStaffPagesButNotManagerPages() throws Exception {
    mvc.perform(get("/staff/bookings").with(user("staff@utepet.vn").roles("STAFF")))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/bookings"));
    mvc.perform(get("/staff/orders").with(user("staff@utepet.vn").roles("STAFF")))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/orders"));
    mvc.perform(get("/manager/dashboard").with(user("staff@utepet.vn").roles("STAFF")))
        .andExpect(status().isForbidden());
  }

  @Test
  void managerCanRenderAllOperationsPages() throws Exception {
    for (String path : new String[] {"/staff/bookings", "/staff/orders", "/manager/dashboard", "/manager/services", "/manager/slots", "/manager/promotions"}) {
      mvc.perform(get(path).with(user("manager@utepet.vn").roles("MANAGER"))).andExpect(status().isOk());
    }
  }
}
