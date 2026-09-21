// File: src/main/java/com/utepetshop/dto/PetRequest.java
package com.utepetshop.dto;

import com.utepetshop.entity.PetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetRequest {
  @NotBlank(message = "Tên thú cưng không được để trống")
  private String name;

  @NotNull(message = "Loài thú cưng không được để trống")
  private PetType petType;

  private String breed;
  private String gender;
  private LocalDate birthday;
  private BigDecimal weight;
  private String imageUrl;
  private String note;
}
