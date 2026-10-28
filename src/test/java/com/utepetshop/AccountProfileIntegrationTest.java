package com.utepetshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.utepetshop.entity.Address;
import com.utepetshop.entity.User;
import com.utepetshop.repository.AddressRepository;
import com.utepetshop.repository.UserRepository;
import com.utepetshop.service.AccountProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountProfileIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private UserRepository users;
  @Autowired private AddressRepository addresses;
  @Autowired private AccountProfileService profiles;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void profilePageRequiresLoginAndRendersForCustomer() throws Exception {
    mvc.perform(get("/account/profile")).andExpect(status().is3xxRedirection());

    mvc.perform(
            get("/account/profile")
                .with(user("customer@utepet.vn").roles("CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(view().name("account/profile"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Hồ sơ tài khoản")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Sổ địa chỉ")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Đổi mật khẩu")));
  }

  @Test
  void customerCanManageProfilePasswordAndOwnedAddresses() {
    User customer = users.findByEmail("customer@utepet.vn").orElseThrow();
    User other = users.findByEmail("manager@utepet.vn").orElseThrow();

    profiles.updateProfile(customer, "Khách hàng mới", "0901234567");
    assertThat(customer.getFullName()).isEqualTo("Khách hàng mới");
    assertThat(customer.getPhone()).isEqualTo("0901234567");

    profiles.changePassword(customer, "Password@123", "NewPassword@123", "NewPassword@123");
    assertThat(passwordEncoder.matches("NewPassword@123", customer.getPassword())).isTrue();

    Address first =
        profiles.addAddress(
            customer,
            "Khách hàng mới",
            "0901234567",
            "01 Võ Văn Ngân",
            "Linh Chiểu",
            "Thủ Đức",
            "TP.HCM",
            false);
    assertThat(first.isDefaultAddress()).isTrue();

    Address second =
        profiles.addAddress(
            customer,
            "Khách hàng mới",
            "0901234567",
            "12 Lê Lợi",
            "Bến Nghé",
            "Quận 1",
            "TP.HCM",
            true);

    assertThat(second.isDefaultAddress()).isTrue();
    assertThat(addresses.findById(first.getId()).orElseThrow().isDefaultAddress()).isFalse();

    assertThatThrownBy(() -> profiles.deleteAddress(other, second.getId()))
        .isInstanceOf(IllegalArgumentException.class);

    profiles.deleteAddress(customer, second.getId());
    assertThat(addresses.findById(second.getId())).isEmpty();
    assertThat(addresses.findById(first.getId()).orElseThrow().isDefaultAddress()).isTrue();
  }
}
