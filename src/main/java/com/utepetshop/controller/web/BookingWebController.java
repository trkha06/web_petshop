// File: src/main/java/com/utepetshop/controller/web/BookingWebController.java
package com.utepetshop.controller.web;

import com.utepetshop.dto.BookingCreateRequest;
import com.utepetshop.dto.BookingResponse;
import com.utepetshop.entity.User;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.service.BookingService;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.PetCareServiceCatalog;
import com.utepetshop.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingWebController {
  private final BookingService bookingService;
  private final PetService petService;
  private final PetCareServiceCatalog serviceCatalog;
  private final BranchRepository branchRepository;
  private final CurrentUser currentUser;

  @GetMapping("/create")
  public String createForm(
      @RequestParam(required = false) Long serviceId,
      Model model) {
    User user = currentUser.required();
    model.addAttribute("pets", petService.getMyActivePets(user));
    model.addAttribute("services", serviceCatalog.getAllActiveServices());
    model.addAttribute("branches", branchRepository.findAll());
    model.addAttribute("selectedServiceId", serviceId);
    model.addAttribute("bookingRequest", new BookingCreateRequest());
    return "bookings/create";
  }

  @PostMapping("/create")
  public String handleCreate(
      @ModelAttribute BookingCreateRequest request,
      RedirectAttributes ra) {
    try {
      User user = currentUser.required();
      BookingResponse booking = bookingService.createBooking(request, user);
      ra.addFlashAttribute("success", "Đặt lịch thành công! Mã lịch hẹn: " + booking.getCode());
      return "redirect:/bookings/" + booking.getId();
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/bookings/create";
    }
  }

  @GetMapping
  public String list(Model model) {
    User user = currentUser.required();
    model.addAttribute("bookings", bookingService.getBookingsByUser(user));
    return "bookings/list";
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable Long id, Model model) {
    User user = currentUser.required();
    model.addAttribute("booking", bookingService.getBookingDetail(id, user, false));
    return "bookings/detail";
  }

  @PostMapping("/{id}/cancel")
  public String cancel(@PathVariable Long id, RedirectAttributes ra) {
    try {
      User user = currentUser.required();
      bookingService.cancelBooking(id, user, false);
      ra.addFlashAttribute("success", "Đã hủy lịch hẹn thành công và giải phóng khung giờ!");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/bookings/" + id;
  }
}
