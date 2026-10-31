// File: src/main/java/com/utepetshop/controller/web/StaffBookingWebController.java
package com.utepetshop.controller.web;

import com.utepetshop.dto.BookingResponse;
import com.utepetshop.entity.BookingStatus;
import com.utepetshop.entity.StaffProfile;
import com.utepetshop.entity.User;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.BookingRepository;
import com.utepetshop.service.BookingService;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.StaffProfileService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingWebController {
  private final BookingService bookingService;
  private final StaffProfileService staffProfileService;
  private final BranchRepository branchRepository;
  private final BookingRepository bookingRepository;
  private final CurrentUser currentUser;

  @GetMapping
  public String listBookings(@RequestParam(required = false) Long branchId, Model model) {
    User user = currentUser.required();
    boolean hasSystemManagementAccess = staffProfileService.hasSystemManagementAccess(user);
    if (!hasSystemManagementAccess) {
      branchId = staffProfileService.getAssignedBranch(user).getId();
    }
    List<BookingResponse> bookings =
        (branchId != null && branchId > 0)
            ? bookingService.getBookingsByBranch(branchId)
            : bookingService.getAllBookings();

    model.addAttribute("bookings", bookings);
    model.addAttribute("branches", branchRepository.findAll());
    model.addAttribute("selectedBranchId", branchId != null ? branchId : 0L);
    model.addAttribute("hasSystemManagementAccess", hasSystemManagementAccess);
    model.addAttribute("statuses", BookingStatus.values());
    return "staff/bookings";
  }

  @PostMapping("/{id}/status")
  public String updateStatus(
      @PathVariable Long id,
      @RequestParam BookingStatus status,
      @RequestParam(required = false) String note,
      RedirectAttributes ra) {
    try {
      User user = currentUser.required();
      Long branchId =
          bookingRepository
              .findById(id)
              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn"))
              .getBranch()
              .getId();
      staffProfileService.requireBranchAccess(user, branchId);
      bookingService.updateStatus(id, status, note);
      ra.addFlashAttribute("success", "Đã cập nhật trạng thái lịch hẹn #" + id + " thành " + status);
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/staff/bookings";
  }
}
