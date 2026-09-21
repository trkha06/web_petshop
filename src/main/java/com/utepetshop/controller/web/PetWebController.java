// File: src/main/java/com/utepetshop/controller/web/PetWebController.java
package com.utepetshop.controller.web;

import com.utepetshop.dto.PetRequest;
import com.utepetshop.dto.PetResponse;
import com.utepetshop.entity.PetType;
import com.utepetshop.entity.User;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetWebController {
  private final PetService petService;
  private final CurrentUser currentUser;

  @GetMapping
  public String list(Model model) {
    User user = currentUser.required();
    model.addAttribute("pets", petService.getMyActivePets(user));
    return "pets/list";
  }

  @GetMapping("/new")
  public String form(Model model) {
    model.addAttribute("pet", new PetRequest());
    model.addAttribute("petTypes", PetType.values());
    model.addAttribute("isEdit", false);
    return "pets/form";
  }

  @PostMapping("/new")
  public String create(@ModelAttribute PetRequest request, RedirectAttributes ra) {
    try {
      User user = currentUser.required();
      petService.createPet(request, user);
      ra.addFlashAttribute("success", "Đã thêm hồ sơ thú cưng thành công!");
      return "redirect:/pets";
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/pets/new";
    }
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long id, Model model) {
    User user = currentUser.required();
    PetResponse pet = petService.getPetById(id, user);
    PetRequest req =
        PetRequest.builder()
            .name(pet.getName())
            .petType(pet.getPetType())
            .breed(pet.getBreed())
            .gender(pet.getGender())
            .birthday(pet.getBirthday())
            .weight(pet.getWeight())
            .imageUrl(pet.getImageUrl())
            .note(pet.getNote())
            .build();
    model.addAttribute("pet", req);
    model.addAttribute("petId", id);
    model.addAttribute("petTypes", PetType.values());
    model.addAttribute("isEdit", true);
    return "pets/form";
  }

  @PostMapping("/{id}/edit")
  public String update(
      @PathVariable Long id, @ModelAttribute PetRequest request, RedirectAttributes ra) {
    try {
      User user = currentUser.required();
      petService.updatePet(id, request, user);
      ra.addFlashAttribute("success", "Đã cập nhật thông tin thú cưng!");
      return "redirect:/pets";
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/pets/" + id + "/edit";
    }
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes ra) {
    try {
      User user = currentUser.required();
      petService.deletePet(id, user);
      ra.addFlashAttribute("success", "Đã xóa hồ sơ thú cưng!");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/pets";
  }
}
