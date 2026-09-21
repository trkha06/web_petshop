// File: src/main/java/com/utepetshop/controller/api/StaffBookingApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.BookingResponse;
import com.utepetshop.dto.BookingStatusUpdateRequest;
import com.utepetshop.entity.StaffProfile;
import com.utepetshop.entity.User;
import com.utepetshop.service.BookingService;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.StaffProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingApiController {
  private final BookingService bookingService;
  private final StaffProfileService staffProfileService;
  private final CurrentUser currentUser;

  @GetMapping
  public ResponseEntity<List<BookingResponse>> getBranchBookings(
      @RequestParam(required = false) Long branchId) {
    User user = currentUser.required();
    if (branchId == null) {
      Optional<StaffProfile> profile = staffProfileService.getStaffProfile(user);
      if (profile.isPresent()) {
        branchId = profile.get().getBranch().getId();
      }
    }

    if (branchId != null) {
      return ResponseEntity.ok(bookingService.getBookingsByBranch(branchId));
    }
    return ResponseEntity.ok(bookingService.getAllBookings());
  }

  @PutMapping("/{id}/status")
  public ResponseEntity<BookingResponse> updateBookingStatus(
      @PathVariable Long id, @Valid @RequestBody BookingStatusUpdateRequest request) {
    return ResponseEntity.ok(bookingService.updateStatus(id, request.getStatus(), request.getNote()));
  }
}
