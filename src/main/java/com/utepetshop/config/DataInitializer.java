package com.utepetshop.config;

import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
  @Bean
  CommandLineRunner seed(
      RoleRepository roles,
      UserRepository users,
      CategoryRepository categories,
      BrandRepository brands,
      ProductRepository products,
      BranchRepository branches,
      InventoryRepository inventories,
      PasswordEncoder encoder) {
    return args -> {
      if (users.count() > 0) return;
      for (RoleName n : RoleName.values()) roles.save(new Role(n));
      createUser(users, roles, encoder, "customer@utepet.vn", "Khách hàng Demo", RoleName.CUSTOMER);
      createUser(users, roles, encoder, "staff@utepet.vn", "Nhân viên Demo", RoleName.STAFF);
      createUser(users, roles, encoder, "manager@utepet.vn", "Quản lý Demo", RoleName.MANAGER);
      createUser(users, roles, encoder, "admin@utepet.vn", "Quản trị viên", RoleName.ADMIN);
      Category food = categories.save(new Category("Thức ăn"));
      Category care = categories.save(new Category("Chăm sóc"));
      Brand royal = brands.save(new Brand("Royal Canin"));
      Brand pet = brands.save(new Brand("UTE Pet"));
      Branch q1 = branches.save(new Branch("UTE Pet Quận 1", "12 Lê Lợi, Quận 1"));
      Branch td = branches.save(new Branch("UTE Pet Thủ Đức", "01 Võ Văn Ngân, Thủ Đức"));
      Product p1 =
          product(
              products,
              "DOG-001",
              "Hạt dinh dưỡng cho chó",
              "Công thức giàu đạm, phù hợp chó trưởng thành",
              new BigDecimal("245000"),
              food,
              royal,
              PetType.DOG,
              "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&w=800&q=80");
      Product p2 =
          product(
              products,
              "CAT-002",
              "Pate cá hồi cho mèo",
              "Bữa ăn mềm mịn, thơm ngon cho mèo",
              new BigDecimal("39000"),
              food,
              pet,
              PetType.CAT,
              "https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=800&q=80");
      Product p3 =
          product(
              products,
              "CARE-003",
              "Sữa tắm dịu nhẹ",
              "Sữa tắm pH cân bằng cho thú cưng",
              new BigDecimal("129000"),
              care,
              pet,
              PetType.OTHER,
              "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=800&q=80");
      for (Product p : List.of(p1, p2, p3)) {
        inventories.save(new Inventory(p, q1, 24));
        inventories.save(new Inventory(p, td, 10));
      }
    };
  }

  private void createUser(
      UserRepository users,
      RoleRepository roles,
      PasswordEncoder e,
      String email,
      String name,
      RoleName role) {
    User u = new User();
    u.setEmail(email);
    u.setFullName(name);
    u.setPassword(e.encode("Password@123"));
    u.getRoles().add(roles.findByName(role).orElseThrow());
    users.save(u);
  }

  private Product product(
      ProductRepository repo,
      String sku,
      String name,
      String desc,
      BigDecimal price,
      Category cat,
      Brand brand,
      PetType type,
      String img) {
    Product p = new Product();
    p.setSku(sku);
    p.setName(name);
    p.setDescription(desc);
    p.setPrice(price);
    p.setCategory(cat);
    p.setBrand(brand);
    p.setPetType(type);
    p.setImageUrl(img);
    return repo.save(p);
  }
}
