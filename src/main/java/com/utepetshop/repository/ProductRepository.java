package com.utepetshop.repository;

import com.utepetshop.entity.*;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Optional<Product> findBySkuIgnoreCase(String sku);

  @Query(
      "select p from Product p where p.active=true and (:keyword is null or lower(p.name) like lower(concat('%',:keyword,'%'))) and (:category is null or p.category.id=:category) and (:brand is null or p.brand.id=:brand) and (:petType is null or p.petType=:petType) and (:minPrice is null or p.price>=:minPrice) and (:maxPrice is null or p.price<=:maxPrice) and (:branch is null or exists (select i from Inventory i where i.product=p and i.branch.id=:branch and (:inStock=false or i.quantity>0))) and (:branch is not null or :inStock=false or exists (select i from Inventory i where i.product=p and i.quantity>0))")
  Page<Product> search(
      @Param("keyword") String keyword,
      @Param("category") Long category,
      @Param("brand") Long brand,
      @Param("petType") PetType petType,
      @Param("minPrice") java.math.BigDecimal minPrice,
      @Param("maxPrice") java.math.BigDecimal maxPrice,
      @Param("branch") Long branch,
      @Param("inStock") boolean inStock,
      Pageable pageable);
}
