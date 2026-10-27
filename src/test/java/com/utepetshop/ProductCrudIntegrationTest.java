package com.utepetshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.utepetshop.entity.Brand;
import com.utepetshop.entity.Category;
import com.utepetshop.entity.Product;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.BrandRepository;
import com.utepetshop.repository.CategoryRepository;
import com.utepetshop.repository.InventoryRepository;
import com.utepetshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductCrudIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ProductRepository products;
  @Autowired private CategoryRepository categories;
  @Autowired private BrandRepository brands;
  @Autowired private BranchRepository branches;
  @Autowired private InventoryRepository inventories;

  @Test
  void managerAndAdminCanOpenProductManagementButCustomerCannot() throws Exception {
    mvc.perform(get("/manager/products").with(user("manager@utepet.vn").roles("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(view().name("manager/products/list"));
    mvc.perform(get("/manager/products/new").with(user("admin@utepet.vn").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("manager/products/form"));
    mvc.perform(get("/manager/products").with(user("customer@utepet.vn").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void managerCanCreateUpdateStopAndResumeProduct() throws Exception {
    Category category = categories.findAll().getFirst();
    Brand brand = brands.findAll().getFirst();

    mvc.perform(
            post("/manager/products")
                .with(user("manager@utepet.vn").roles("MANAGER"))
                .with(csrf())
                .param("sku", " test-100 ")
                .param("name", "Bánh thưởng cho thú cưng")
                .param("description", "Gói dùng thử")
                .param("price", "85000")
                .param("categoryId", category.getId().toString())
                .param("brandId", brand.getId().toString())
                .param("petType", "OTHER")
                .param("imageUrl", "https://example.com/product.jpg"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/manager/products"));

    Product created = products.findBySkuIgnoreCase("TEST-100").orElseThrow();
    assertThat(created.getName()).isEqualTo("Bánh thưởng cho thú cưng");
    assertThat(created.isActive()).isTrue();
    assertThat(inventories.findByProductId(created.getId())).hasSize((int) branches.count());
    assertThat(inventories.findByProductId(created.getId()))
        .allMatch(inventory -> inventory.getQuantity() == 0);

    mvc.perform(
            post("/manager/products")
                .with(user("manager@utepet.vn").roles("MANAGER"))
                .with(csrf())
                .param("id", created.getId().toString())
                .param("sku", "TEST-100")
                .param("name", "Bánh thưởng vị mới")
                .param("description", "Đã cập nhật")
                .param("price", "99000")
                .param("categoryId", category.getId().toString())
                .param("brandId", brand.getId().toString())
                .param("petType", "DOG")
                .param("imageUrl", "https://example.com/product-new.jpg"))
        .andExpect(status().is3xxRedirection());

    Product updated = products.findById(created.getId()).orElseThrow();
    assertThat(updated.getName()).isEqualTo("Bánh thưởng vị mới");
    assertThat(updated.getPrice().toPlainString()).isEqualTo("99000");

    mvc.perform(
            post("/manager/products/{id}/status", created.getId())
                .with(user("manager@utepet.vn").roles("MANAGER"))
                .with(csrf())
                .param("active", "false"))
        .andExpect(status().is3xxRedirection());
    assertThat(products.findById(created.getId()).orElseThrow().isActive()).isFalse();

    mvc.perform(
            post("/manager/products/{id}/status", created.getId())
                .with(user("admin@utepet.vn").roles("ADMIN"))
                .with(csrf())
                .param("active", "true"))
        .andExpect(status().is3xxRedirection());
    assertThat(products.findById(created.getId()).orElseThrow().isActive()).isTrue();
  }
}
