// File: src/main/java/com/utepetshop/service/StaffProfileService.java
package com.utepetshop.service;

import com.utepetshop.entity.Branch;
import com.utepetshop.entity.StaffProfile;
import com.utepetshop.entity.User;
import com.utepetshop.entity.RoleName;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.RoleRepository;
import com.utepetshop.repository.StaffProfileRepository;
import com.utepetshop.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffProfileService {
  private final StaffProfileRepository staffProfileRepository;
  private final BranchRepository branchRepository;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public Optional<StaffProfile> getStaffProfile(User user) {
    return staffProfileRepository.findByUserId(user.getId());
  }

  @Transactional(readOnly = true)
  public Branch getAssignedBranch(User user) {
    return staffProfileRepository
        .findByUserId(user.getId())
        .map(StaffProfile::getBranch)
        .orElseThrow(() -> new BusinessException("Tài khoản nhân viên chưa được gán chi nhánh"));
  }

  @Transactional(readOnly = true)
  public boolean hasSystemManagementAccess(User user) {
    return user.getRoles().stream()
        .map(role -> role.getName())
        .anyMatch(role -> role == RoleName.MANAGER || role == RoleName.ADMIN);
  }

  @Transactional(readOnly = true)
  public void requireBranchAccess(User user, Long branchId) {
    if (hasSystemManagementAccess(user)) return;
    Branch branch = getAssignedBranch(user);
    if (!branch.getId().equals(branchId)) {
      throw new BusinessException("Bạn chỉ được xử lý dữ liệu của chi nhánh " + branch.getName());
    }
  }

  @Transactional
  public StaffProfile assignStaffToBranch(Long userId, Long branchId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
    boolean isStaff = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.STAFF);
    if (!isStaff) throw new BusinessException("Chỉ có thể gán chi nhánh cho tài khoản STAFF");

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

  @Transactional
  public StaffProfile createStaff(
      String fullName, String email, String phone, String password, Long branchId) {
    String normalizedName = fullName == null ? "" : fullName.trim();
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
    String normalizedPhone = phone == null || phone.isBlank() ? null : phone.trim();
    if (normalizedName.isBlank() || normalizedName.length() > 255)
      throw new BusinessException("Vui lòng nhập họ tên nhân viên hợp lệ");
    if (!normalizedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
      throw new BusinessException("Email nhân viên không hợp lệ");
    if (userRepository.findByEmail(normalizedEmail).isPresent())
      throw new BusinessException("Email này đã tồn tại");
    if (password == null || password.length() < 8)
      throw new BusinessException("Mật khẩu nhân viên phải có ít nhất 8 ký tự");
    if (normalizedPhone != null && normalizedPhone.length() > 20)
      throw new BusinessException("Số điện thoại không hợp lệ");

    Branch branch =
        branchRepository
            .findById(branchId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy chi nhánh"));
    User user = new User();
    user.setFullName(normalizedName);
    user.setEmail(normalizedEmail);
    user.setPhone(normalizedPhone);
    user.setPassword(passwordEncoder.encode(password));
    user.setActive(true);
    user.getRoles().add(roleRepository.findByName(RoleName.STAFF).orElseThrow());
    User savedUser = userRepository.save(user);
    return staffProfileRepository.save(new StaffProfile(savedUser, branch));
  }
}
