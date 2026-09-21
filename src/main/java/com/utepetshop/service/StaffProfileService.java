// File: src/main/java/com/utepetshop/service/StaffProfileService.java
package com.utepetshop.service;

import com.utepetshop.entity.Branch;
import com.utepetshop.entity.StaffProfile;
import com.utepetshop.entity.User;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.StaffProfileRepository;
import com.utepetshop.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffProfileService {
  private final StaffProfileRepository staffProfileRepository;
  private final BranchRepository branchRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Optional<StaffProfile> getStaffProfile(User user) {
    return staffProfileRepository.findByUserId(user.getId());
  }

  @Transactional
  public StaffProfile assignStaffToBranch(Long userId, Long branchId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));

    Branch branch =
        branchRepository
            .findById(branchId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy chi nhánh"));

    StaffProfile profile =
        staffProfileRepository
            .findByUserId(userId)
            .orElseGet(() -> new StaffProfile(user, branch));

    profile.setBranch(branch);
    return staffProfileRepository.save(profile);
  }
}
