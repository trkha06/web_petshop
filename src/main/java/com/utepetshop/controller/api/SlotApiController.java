// File: src/main/java/com/utepetshop/controller/api/SlotApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.ServiceSlotResponse;
import com.utepetshop.service.SlotManagementService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SlotApiController {
  private final SlotManagementService slotService;

  @GetMapping("/branches/{branchId}/services/{serviceId}/slots")
  public ResponseEntity<List<ServiceSlotResponse>> getSlots(
      @PathVariable Long branchId,
      @PathVariable Long serviceId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(slotService.getAvailableSlots(branchId, serviceId, date));
  }
}
