// File: src/main/java/com/utepetshop/dto/PetResponse.java
package com.utepetshop.dto;

import com.utepetshop.entity.Pet;
import com.utepetshop.entity.PetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetResponse {
  private Long id;
  private Long userId;
  private String name;
  private PetType petType;
  private String breed;
  private String gender;
  private LocalDate birthday;
  private BigDecimal weight;
  private String imageUrl;
  private String note;
  private boolean active;

  public static PetResponse fromEntity(Pet p) {
    return PetResponse.builder()
        .id(p.getId())
        .userId(p.getUser() != null ? p.getUser().getId() : null)
        .name(p.getName())
        .petType(p.getPetType())
        .breed(p.getBreed())
        .gender(p.getGender())
        .birthday(p.getBirthday())
        .weight(p.getWeight())
        .imageUrl(p.getImageUrl())
        .note(p.getNote())
        .active(p.isActive())
        .build();
  }
}
