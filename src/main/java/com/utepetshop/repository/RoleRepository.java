package com.utepetshop.repository;

import com.utepetshop.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(RoleName name);
}
