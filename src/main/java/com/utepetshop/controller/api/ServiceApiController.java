// File: src/main/java/com/utepetshop/controller/api/ServiceApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.ServiceDto;
import com.utepetshop.service.PetCareServiceCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceApiController {
  private final PetCareServiceCatalog serviceCatalog;

  @GetMapping
  public ResponseEntity<List<ServiceDto>> getAllServices() {
    return ResponseEntity.ok(serviceCatalog.getAllActiveServices());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ServiceDto> getServiceDetail(@PathVariable Long id) {
    return ResponseEntity.ok(serviceCatalog.getServiceById(id));
  }
}
