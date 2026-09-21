// File: src/main/java/com/utepetshop/dto/ServiceDto.java
package com.utepetshop.dto;

import com.utepetshop.entity.PetType;
import com.utepetshop.entity.Service;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDto {
  private Long id;
  private String name;
  private String description;
  private BigDecimal price;
  private int durationMinutes;
  private String imageUrl;
  private PetType petType;
  private boolean active;

  public static ServiceDto fromEntity(Service s) {
    return ServiceDto.builder()
        .id(s.getId())
        .name(s.getName())
        .description(s.getDescription())
        .price(s.getPrice())
        .durationMinutes(s.getDurationMinutes())
        .imageUrl(s.getImageUrl())
        .petType(s.getPetType())
        .active(s.isActive())
        .build();
  }
}
