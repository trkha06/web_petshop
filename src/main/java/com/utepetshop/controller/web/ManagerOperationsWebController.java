// File: src/main/java/com/utepetshop/controller/web/ManagerOperationsWebController.java
package com.utepetshop.controller.web;

import com.utepetshop.dto.DashboardAnalyticsDto;
import com.utepetshop.dto.PromotionDto;
import com.utepetshop.dto.ServiceRequest;
import com.utepetshop.dto.SlotGenerateRequest;
import com.utepetshop.entity.DiscountType;
import com.utepetshop.entity.PetType;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.service.AnalyticsDashboardService;
import com.utepetshop.service.PetCareServiceCatalog;
import com.utepetshop.service.PromotionService;
import com.utepetshop.service.SlotManagementService;
import java.time.LocalDate;
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
}
