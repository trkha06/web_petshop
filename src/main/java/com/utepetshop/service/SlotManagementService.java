// File: src/main/java/com/utepetshop/service/SlotManagementService.java
package com.utepetshop.service;

import com.utepetshop.dto.ServiceSlotResponse;
import com.utepetshop.dto.SlotGenerateRequest;
import com.utepetshop.entity.Branch;
import com.utepetshop.entity.ServiceSlot;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.ServiceRepository;
import com.utepetshop.repository.ServiceSlotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotManagementService {
  private final ServiceSlotRepository slotRepository;
  private final BranchRepository branchRepository;
  private final ServiceRepository serviceRepository;

  @Transactional(readOnly = true)
  public List<ServiceSlotResponse> getAvailableSlots(
      Long branchId, Long serviceId, LocalDate date) {
    return slotRepository
        .findByBranchIdAndServiceIdAndSlotDateAndActiveTrueOrderByStartTimeAsc(
            branchId, serviceId, date)
        .stream()
        .map(ServiceSlotResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ServiceSlotResponse> getSlotsByBranchAndDate(Long branchId, LocalDate date) {
    return slotRepository.findByBranchIdAndSlotDateOrderByStartTimeAsc(branchId, date).stream()
        .map(ServiceSlotResponse::fromEntity)
        .toList();
  }

  /**
   * Sinh slot hàng loạt theo khung giờ và khoảng cách (intervalMinutes).
   * Đảm bảo không trùng lặp (branch, service, date, startTime).
   */
  @Transactional
  public int generateSlots(SlotGenerateRequest req) {
    if (req.getStartDate() == null
        || req.getEndDate() == null
        || req.getStartDate().isBefore(LocalDate.now())) {
      throw new BusinessException("Ngày tạo slot phải từ hôm nay trở đi!");
    }
    if (req.getEndDate().isBefore(req.getStartDate())) {
      throw new BusinessException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu!");
    }
    if (req.getStartTime() == null
        || req.getEndTime() == null
        || !req.getEndTime().isAfter(req.getStartTime())) {
      throw new BusinessException("Khung giờ slot không hợp lệ!");
    }
    if (req.getCapacity() <= 0 || req.getIntervalMinutes() < 15) {
      throw new BusinessException("Sức chứa và khoảng cách slot không hợp lệ!");
    }
    Branch branch =
        branchRepository
            .findById(req.getBranchId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy chi nhánh"));

    com.utepetshop.entity.Service service =
        serviceRepository
            .findById(req.getServiceId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ"));

    int interval = req.getIntervalMinutes() > 0 ? req.getIntervalMinutes() : service.getDurationMinutes();
    if (interval <= 0) interval = 60;

    List<ServiceSlot> newSlots = new ArrayList<>();
    LocalDate currentDate = req.getStartDate();

    while (!currentDate.isAfter(req.getEndDate())) {
      LocalTime currentTime = req.getStartTime();
      while (currentTime.plusMinutes(interval).isBefore(req.getEndTime())
          || currentTime.plusMinutes(interval).equals(req.getEndTime())) {
        LocalTime endTime = currentTime.plusMinutes(interval);

        boolean exists =
            slotRepository.existsByBranchIdAndServiceIdAndSlotDateAndStartTime(
                branch.getId(), service.getId(), currentDate, currentTime);

        if (!exists) {
          ServiceSlot slot =
              new ServiceSlot(
                  branch,
                  service,
                  currentDate,
                  currentTime,
                  endTime,
                  req.getCapacity() > 0 ? req.getCapacity() : 1);
          newSlots.add(slot);
        }

        currentTime = endTime;
      }
      currentDate = currentDate.plusDays(1);
    }

    if (!newSlots.isEmpty()) {
      slotRepository.saveAll(newSlots);
      log.info("Generated {} new slots for branch {} and service {}", newSlots.size(), branch.getName(), service.getName());
    }

    return newSlots.size();
  }

  @Transactional
  public void toggleSlotActive(Long slotId) {
    ServiceSlot slot =
        slotRepository
            .findById(slotId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy khung giờ #" + slotId));
    slot.setActive(!slot.isActive());
    slotRepository.save(slot);
  }
}
