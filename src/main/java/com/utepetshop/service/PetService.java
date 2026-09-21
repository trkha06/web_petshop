// File: src/main/java/com/utepetshop/service/PetService.java
package com.utepetshop.service;

import com.utepetshop.dto.PetRequest;
import com.utepetshop.dto.PetResponse;
import com.utepetshop.entity.Pet;
import com.utepetshop.entity.User;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.PetRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetService {
  private final PetRepository petRepository;

  @Transactional(readOnly = true)
  public List<PetResponse> getMyActivePets(User user) {
    return petRepository.findByUserIdAndActiveTrueOrderByIdDesc(user.getId()).stream()
        .map(PetResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PetResponse> getAllMyPets(User user) {
    return petRepository.findByUserIdOrderByIdDesc(user.getId()).stream()
        .map(PetResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public PetResponse getPetById(Long petId, User user) {
    Pet pet =
        petRepository
            .findByIdAndUserId(petId, user.getId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy thú cưng của bạn"));
    return PetResponse.fromEntity(pet);
  }

  @Transactional
  public PetResponse createPet(PetRequest req, User user) {
    Pet pet = new Pet();
    pet.setUser(user);
    pet.setName(req.getName());
    pet.setPetType(req.getPetType());
    pet.setBreed(req.getBreed());
    pet.setGender(req.getGender());
    pet.setBirthday(req.getBirthday());
    pet.setWeight(req.getWeight());
    pet.setImageUrl(
        req.getImageUrl() != null && !req.getImageUrl().isBlank()
            ? req.getImageUrl()
            : "https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=800&q=80");
    pet.setNote(req.getNote());
    pet.setActive(true);
    pet.setCreatedAt(LocalDateTime.now());
    pet.setUpdatedAt(LocalDateTime.now());

    Pet saved = petRepository.save(pet);
    return PetResponse.fromEntity(saved);
  }

  @Transactional
  public PetResponse updatePet(Long petId, PetRequest req, User user) {
    Pet pet =
        petRepository
            .findByIdAndUserId(petId, user.getId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy thú cưng hoặc bạn không có quyền sửa!"));

    pet.setName(req.getName());
    pet.setPetType(req.getPetType());
    pet.setBreed(req.getBreed());
    pet.setGender(req.getGender());
    pet.setBirthday(req.getBirthday());
    pet.setWeight(req.getWeight());
    if (req.getImageUrl() != null && !req.getImageUrl().isBlank()) {
      pet.setImageUrl(req.getImageUrl());
    }
    pet.setNote(req.getNote());
    pet.setUpdatedAt(LocalDateTime.now());

    Pet updated = petRepository.save(pet);
    return PetResponse.fromEntity(updated);
  }

  @Transactional
  public void deletePet(Long petId, User user) {
    Pet pet =
        petRepository
            .findByIdAndUserId(petId, user.getId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy thú cưng hoặc bạn không có quyền xóa!"));

    // Soft delete để không làm hỏng lịch sử booking
    pet.setActive(false);
    pet.setUpdatedAt(LocalDateTime.now());
    petRepository.save(pet);
  }
}
