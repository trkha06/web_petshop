// File: src/main/java/com/utepetshop/controller/web/ManagerOperationsWebController.java
package com.utepetshop.controller.web;

import com.utepetshop.dto.DashboardAnalyticsDto;
import com.utepetshop.dto.PromotionDto;
import com.utepetshop.dto.ServiceRequest;
import com.utepetshop.dto.SlotGenerateRequest;
import com.utepetshop.entity.DiscountType;
import com.utepetshop.entity.PetType;
import com.utepetshop.entity.RoleName;
import com.utepetshop.entity.User;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.StaffProfileRepository;
import com.utepetshop.repository.UserRepository;
import com.utepetshop.service.AnalyticsDashboardService;
import com.utepetshop.service.PetCareServiceCatalog;
import com.utepetshop.service.PromotionService;
import com.utepetshop.service.SlotManagementService;
import com.utepetshop.service.StaffProfileService;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerOperationsWebController {
  private final AnalyticsDashboardService analyticsService;
  private final PetCareServiceCatalog serviceCatalog;
  private final SlotManagementService slotService;
  private final BranchRepository branchRepository;
  private final PromotionService promotionService;
  private final UserRepository userRepository;
  private final StaffProfileRepository staffProfileRepository;
  private final StaffProfileService staffProfileService;

  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    DashboardAnalyticsDto data = analyticsService.getDashboardData();
    model.addAttribute("data", data);
    return "manager/dashboard";
  }

  @GetMapping("/services")
  public String listServices(Model model) {
    model.addAttribute("services", serviceCatalog.getAllServices());
    model.addAttribute("newService", new ServiceRequest());
    model.addAttribute("petTypes", PetType.values());
    return "manager/services/list";
  }

  @PostMapping("/services")
  public String createService(@ModelAttribute ServiceRequest req, RedirectAttributes ra) {
    try {
      serviceCatalog.createService(req);
      ra.addFlashAttribute("success", "Đã thêm dịch vụ mới thành công!");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/services";
  }

  @PostMapping("/services/{id}/toggle")
  public String toggleService(@PathVariable Long id, RedirectAttributes ra) {
    try {
      serviceCatalog.toggleServiceActive(id);
      ra.addFlashAttribute("success", "Đã cập nhật trạng thái dịch vụ!");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/services";
  }

  @GetMapping("/slots")
  public String slotsPage(
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) LocalDate date,
      Model model) {
    if (date == null) date = LocalDate.now();
    model.addAttribute("branches", branchRepository.findAll());
    model.addAttribute("services", serviceCatalog.getAllActiveServices());
    model.addAttribute("selectedBranchId", branchId);
    model.addAttribute("selectedDate", date);
    if (branchId != null) {
      model.addAttribute("slots", slotService.getSlotsByBranchAndDate(branchId, date));
    }
    model.addAttribute("generateRequest", new SlotGenerateRequest());
    return "manager/slots/list";
  }

  @PostMapping("/slots/generate")
  public String generateSlots(@ModelAttribute SlotGenerateRequest req, RedirectAttributes ra) {
    try {
      int count = slotService.generateSlots(req);
      ra.addFlashAttribute("success", "Đã tự động sinh " + count + " khung giờ hẹn!");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/slots?branchId=" + req.getBranchId() + "&date=" + req.getStartDate();
  }

  @GetMapping("/promotions")
  public String promotionsPage(Model model) {
    model.addAttribute("promotions", promotionService.getAllPromotions());
    model.addAttribute("newPromo", new PromotionDto());
    model.addAttribute("discountTypes", DiscountType.values());
    return "manager/promotions/list";
  }

  @PostMapping("/promotions")
  public String createPromotion(@ModelAttribute PromotionDto dto, RedirectAttributes ra) {
    try {
      promotionService.createPromotion(dto);
      ra.addFlashAttribute("success", "Đã tạo mã khuyến mãi mới thành công!");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/promotions";
  }

  @PostMapping("/promotions/{id}/toggle")
  public String togglePromotion(@PathVariable Long id, RedirectAttributes ra) {
    try {
      promotionService.togglePromotionActive(id);
      ra.addFlashAttribute("success", "Đã đổi trạng thái mã khuyến mãi!");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/promotions";
  }

  @GetMapping("/staff")
  public String staffPage(Model model) {
    Map<Long, Long> assignedBranchIds =
        staffProfileRepository.findAllByOrderByUserFullNameAsc().stream()
            .collect(Collectors.toMap(profile -> profile.getUser().getId(), profile -> profile.getBranch().getId()));
    java.util.List<User> staffUsers =
        userRepository.findAll().stream()
            .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.STAFF))
            .sorted(java.util.Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    model.addAttribute("staffUsers", staffUsers);
    model.addAttribute("branches", branchRepository.findAll());
    model.addAttribute("assignedBranchIds", assignedBranchIds);
    return "manager/staff/list";
  }

  @PostMapping("/staff/{userId}/branch")
  public String assignStaffBranch(
      @PathVariable Long userId, @RequestParam Long branchId, RedirectAttributes ra) {
    try {
      staffProfileService.assignStaffToBranch(userId, branchId);
      ra.addFlashAttribute("success", "Đã gán nhân viên vào chi nhánh");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/staff";
  }

  @PostMapping("/staff")
  public String createStaff(
      @RequestParam String fullName,
      @RequestParam String email,
      @RequestParam(required = false) String phone,
      @RequestParam String password,
      @RequestParam Long branchId,
      RedirectAttributes ra) {
    try {
      staffProfileService.createStaff(fullName, email, phone, password, branchId);
      ra.addFlashAttribute("success", "Đã tạo tài khoản nhân viên và gán chi nhánh");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/manager/staff";
  }
}
