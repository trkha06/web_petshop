package com.utepetshop.repository;

import com.utepetshop.entity.Address;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
  List<Address> findByUserIdOrderByDefaultAddressDesc(Long userId);
}
