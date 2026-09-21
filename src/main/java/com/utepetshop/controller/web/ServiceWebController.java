// File: src/main/java/com/utepetshop/controller/web/ServiceWebController.java
package com.utepetshop.controller.web;

import com.utepetshop.dto.ServiceDto;
import com.utepetshop.service.PetCareServiceCatalog;
import com.utepetshop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceWebController {
  private final PetCareServiceCatalog serviceCatalog;
  private final ReviewService reviewService;

  @GetMapping
  public String list(Model model) {
    model.addAttribute("services", serviceCatalog.getAllActiveServices());
    return "services/list";
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable Long id, Model model) {
    ServiceDto service = serviceCatalog.getServiceById(id);
    model.addAttribute("service", service);
    model.addAttribute("reviews", reviewService.getServiceReviews(id));
    return "services/detail";
  }
}
