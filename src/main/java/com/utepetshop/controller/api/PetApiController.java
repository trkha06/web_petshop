// File: src/main/java/com/utepetshop/controller/api/PetApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.PetRequest;
import com.utepetshop.dto.PetResponse;
import com.utepetshop.entity.User;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.PetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetApiController {
  private final PetService petService;
  private final CurrentUser currentUser;

  @GetMapping
  public ResponseEntity<List<PetResponse>> getMyPets() {
    User user = currentUser.required();
    return ResponseEntity.ok(petService.getMyActivePets(user));
  }

  @PostMapping
  public ResponseEntity<PetResponse> createPet(@Valid @RequestBody PetRequest request) {
    User user = currentUser.required();
    return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request, user));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PetResponse> getPetDetail(@PathVariable Long id) {
    User user = currentUser.required();
    return ResponseEntity.ok(petService.getPetById(id, user));
  }

  @PutMapping("/{id}")
  public ResponseEntity<PetResponse> updatePet(
      @PathVariable Long id, @Valid @RequestBody PetRequest request) {
    User user = currentUser.required();
    return ResponseEntity.ok(petService.updatePet(id, request, user));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Map<String, String>> deletePet(@PathVariable Long id) {
    User user = currentUser.required();
    petService.deletePet(id, user);
    return ResponseEntity.ok(Map.of("message", "Đã xóa hồ sơ thú cưng thành công"));
  }
}
