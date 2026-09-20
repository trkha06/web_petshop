package com.utepetshop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecuritySmokeTest {
  @Autowired MockMvc mvc;

  @Test
  void publicProductsAreAvailable() throws Exception {
    mvc.perform(get("/api/products")).andExpect(status().isOk());
  }

  @Test
  void adminApiRejectsNoToken() throws Exception {
    mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
  }
}
