// File: src/main/java/com/utepetshop/controller/api/ManagerAnalyticsApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.*;
import com.utepetshop.service.AnalyticsDashboardService;
import com.utepetshop.service.PetCareServiceCatalog;
import com.utepetshop.service.SlotManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerAnalyticsApiController {
  private final AnalyticsDashboardService analyticsService;
  private final SlotManagementService slotService;
  private final PetCareServiceCatalog serviceCatalog;

  @GetMapping("/dashboard/data")
  public ResponseEntity<DashboardAnalyticsDto> getDashboardData() {
    return ResponseEntity.ok(analyticsService.getDashboardData());
  }

  @GetMapping("/dashboard/kpi")
  public ResponseEntity<Map<String, Object>> getKpis() {
    DashboardAnalyticsDto data = analyticsService.getDashboardData();
    return ResponseEntity.ok(
        Map.of(
            "revenueToday", data.getRevenueToday(),
            "revenueThisMonth", data.getRevenueThisMonth(),
            "totalOrders", data.getTotalOrders(),
            "totalBookings", data.getTotalBookings(),
            "pendingBookings", data.getPendingBookingsCount(),
            "lowStockCount", data.getLowStockCount()));
  }

  @PostMapping("/slots/generate")
  public ResponseEntity<Map<String, Object>> generateSlots(
      @Valid @RequestBody SlotGenerateRequest request) {
    int count = slotService.generateSlots(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "Đã tạo thành công " + count + " khung giờ hẹn", "count", count));
  }

  @GetMapping("/services")
  public ResponseEntity<List<ServiceDto>> getAllServices() {
    return ResponseEntity.ok(serviceCatalog.getAllServices());
  }

  @PostMapping("/services")
  public ResponseEntity<ServiceDto> createService(@Valid @RequestBody ServiceRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalog.createService(request));
  }

  @PutMapping("/services/{id}")
  public ResponseEntity<ServiceDto> updateService(
      @PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
    return ResponseEntity.ok(serviceCatalog.updateService(id, request));
  }

  @PutMapping("/services/{id}/toggle")
  public ResponseEntity<Map<String, String>> toggleService(@PathVariable Long id) {
    serviceCatalog.toggleServiceActive(id);
    return ResponseEntity.ok(Map.of("message", "Đã đổi trạng thái dịch vụ thành công"));
  }
}
