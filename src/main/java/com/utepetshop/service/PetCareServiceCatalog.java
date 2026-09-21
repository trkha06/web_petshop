// File: src/main/java/com/utepetshop/service/PetCareServiceCatalog.java
package com.utepetshop.service;

import com.utepetshop.dto.ServiceDto;
import com.utepetshop.dto.ServiceRequest;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.ServiceRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetCareServiceCatalog {
  private final ServiceRepository serviceRepository;

  @Transactional(readOnly = true)
  public List<ServiceDto> getAllActiveServices() {
    return serviceRepository.findByActiveTrueOrderByIdAsc().stream()
        .map(ServiceDto::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ServiceDto> getAllServices() {
    return serviceRepository.findAll().stream()
        .map(ServiceDto::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public ServiceDto getServiceById(Long id) {
    com.utepetshop.entity.Service service =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ chăm sóc #" + id));
    return ServiceDto.fromEntity(service);
  }

  @Transactional
  public ServiceDto createService(ServiceRequest req) {
    com.utepetshop.entity.Service s = new com.utepetshop.entity.Service();
    s.setName(req.getName());
    s.setDescription(req.getDescription());
    s.setPrice(req.getPrice());
    s.setDurationMinutes(req.getDurationMinutes());
    s.setPetType(req.getPetType());
    s.setImageUrl(
        req.getImageUrl() != null && !req.getImageUrl().isBlank()
            ? req.getImageUrl()
            : "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=800&q=80");
    s.setActive(true);
    s.setCreatedAt(LocalDateTime.now());

    com.utepetshop.entity.Service saved = serviceRepository.save(s);
    return ServiceDto.fromEntity(saved);
  }

  @Transactional
  public ServiceDto updateService(Long id, ServiceRequest req) {
    com.utepetshop.entity.Service s =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ #" + id));

    s.setName(req.getName());
    s.setDescription(req.getDescription());
    s.setPrice(req.getPrice());
    s.setDurationMinutes(req.getDurationMinutes());
    s.setPetType(req.getPetType());
    if (req.getImageUrl() != null && !req.getImageUrl().isBlank()) {
      s.setImageUrl(req.getImageUrl());
    }

    com.utepetshop.entity.Service updated = serviceRepository.save(s);
    return ServiceDto.fromEntity(updated);
  }

  @Transactional
  public void toggleServiceActive(Long id) {
    com.utepetshop.entity.Service s =
        serviceRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ #" + id));
    s.setActive(!s.isActive());
    serviceRepository.save(s);
  }
}
