package com.utepetshop.repository;

import com.utepetshop.entity.User;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
}
