// File: src/main/java/com/utepetshop/service/BookingService.java
package com.utepetshop.service;

import com.utepetshop.dto.BookingCreateRequest;
import com.utepetshop.dto.BookingResponse;
import com.utepetshop.entity.*;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.exception.SlotFullException;
import com.utepetshop.repository.*;
import com.utepetshop.service.event.BookingStatusChangedEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
  private final BookingRepository bookingRepository;
  private final ServiceSlotRepository slotRepository;
  private final PetRepository petRepository;
  private final ServiceRepository serviceRepository;
  private final BranchRepository branchRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Đặt lịch dịch vụ với Transactional READ_COMMITTED và PESSIMISTIC_WRITE lock
   * chống race condition và overbooking tuyệt đối.
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public BookingResponse createBooking(BookingCreateRequest request, User user) {
    log.info("Creating booking for user: {}, slot: {}, pet: {}", user.getEmail(), request.getSlotId(), request.getPetId());

    // 1. Validate Customer sở hữu Pet và Pet đang active
    Pet pet =
        petRepository
            .findById(request.getPetId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin thú cưng"));
    if (!pet.getUser().getId().equals(user.getId())) {
      throw new BusinessException("Bạn không phải chủ sở hữu của thú cưng này!");
    }
    if (!pet.isActive()) {
      throw new BusinessException("Hồ sơ thú cưng hiện không hoạt động!");
    }

    // 2. Load Service, Branch
    com.utepetshop.entity.Service service =
        serviceRepository
            .findById(request.getServiceId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ"));
    if (!service.isActive()) {
      throw new BusinessException("Dịch vụ này hiện đang tạm ngưng phục vụ!");
    }

    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy chi nhánh"));

    // 3. Khóa dòng slot bằng findByIdWithLock(slotId) (PESSIMISTIC_WRITE)
    ServiceSlot slot =
        slotRepository
            .findByIdWithLock(request.getSlotId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy khung giờ hẹn!"));

    // Kiểm tra tính nhất quán giữa slot, branch và service
    if (!slot.getBranch().getId().equals(branch.getId())) {
      throw new BusinessException("Khung giờ hẹn không thuộc chi nhánh đã chọn!");
    }
    if (!slot.getService().getId().equals(service.getId())) {
      throw new BusinessException("Khung giờ hẹn không áp dụng cho dịch vụ này!");
    }
    if (!slot.isActive()) {
      throw new BusinessException("Khung giờ hẹn này đã bị đóng!");
    }
    if (slot.getSlotDate().isBefore(LocalDate.now())) {
      throw new BusinessException("Không thể đặt lịch vào ngày trong quá khứ!");
    }
    if (bookingRepository.existsByUserIdAndSlotIdAndStatusNot(
        user.getId(), slot.getId(), BookingStatus.CANCELLED)) {
      throw new BusinessException("Bạn đã có lịch hẹn đang hiệu lực trong khung giờ này!");
    }

    // 4. Kiểm tra nếu slot.getBookedCount() >= slot.getCapacity() thì ném SlotFullException
    if (slot.getBookedCount() >= slot.getCapacity()) {
      log.warn("Slot {} full: bookedCount={} >= capacity={}", slot.getId(), slot.getBookedCount(), slot.getCapacity());
      throw new SlotFullException("Khung giờ này đã hết chỗ, quý khách vui lòng chọn giờ khác!");
    }

    // 5. Tăng slot.setBookedCount(slot.getBookedCount() + 1)
    slot.setBookedCount(slot.getBookedCount() + 1);
    slotRepository.save(slot);

    // 6. Lưu snapshot giá và tạo Booking với trạng thái PENDING
    Booking booking = new Booking();
    booking.setCode(generateBookingCode());
    booking.setUser(user);
    booking.setPet(pet);
    booking.setService(service);
    booking.setBranch(branch);
    booking.setSlot(slot);
    booking.setTotalPrice(service.getPrice()); // Snapshot giá dịch vụ
    booking.setNote(request.getNote());
    booking.setStatus(BookingStatus.PENDING);
    booking.setCreatedAt(LocalDateTime.now());
    booking.setUpdatedAt(LocalDateTime.now());

    Booking saved = bookingRepository.save(booking);

    // 7. Phát sự kiện để gửi WebSocket Toast realtime
    eventPublisher.publishEvent(
        new BookingStatusChangedEvent(this, saved, "Đặt lịch thành công"));

    log.info("Booking created successfully with code: {}", saved.getCode());
    return BookingResponse.fromEntity(saved);
  }

  /**
   * Logic Hủy (cancelBooking): Chỉ cho phép khi trạng thái là PENDING hoặc CONFIRMED.
   * Khi hủy, giảm bookedCount của slot đi 1.
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public BookingResponse cancelBooking(Long bookingId, User user, boolean isStaffOrAdmin) {
    Booking booking =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt lịch"));

    if (!isStaffOrAdmin && !booking.getUser().getId().equals(user.getId())) {
      throw new BusinessException("Bạn không có quyền hủy lịch hẹn này!");
    }

    if (booking.getStatus() != BookingStatus.PENDING
        && booking.getStatus() != BookingStatus.CONFIRMED) {
      throw new BusinessException(
          "Chỉ có thể hủy lịch khi đang chờ xác nhận hoặc đã xác nhận. Lịch đang thực hiện hoặc đã hoàn thành không thể hủy!");
    }

    // Khóa dòng slot để hoàn lại chỗ
    ServiceSlot slot =
        slotRepository
            .findByIdWithLock(booking.getSlot().getId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy khung giờ liên quan"));

    slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
    slotRepository.save(slot);

    booking.setStatus(BookingStatus.CANCELLED);
    booking.setUpdatedAt(LocalDateTime.now());
    Booking saved = bookingRepository.save(booking);

    eventPublisher.publishEvent(
        new BookingStatusChangedEvent(this, saved, "Hủy lịch thành công"));

    return BookingResponse.fromEntity(saved);
  }

  /**
   * Chuyển trạng thái booking cho Staff / Quản lý:
   * PENDING -> CONFIRMED -> CHECKED_IN -> IN_PROGRESS -> COMPLETED (hoặc CANCELLED)
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public BookingResponse updateStatus(Long bookingId, BookingStatus newStatus, String note) {
    Booking booking =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt lịch"));

    BookingStatus oldStatus = booking.getStatus();
    if (oldStatus == BookingStatus.COMPLETED || oldStatus == BookingStatus.CANCELLED) {
      throw new BusinessException("Lịch hẹn đã kết thúc (" + oldStatus + "), không thể đổi trạng thái!");
    }

    if (newStatus == BookingStatus.CANCELLED) {
      // Hoàn lại chỗ nếu hủy từ PENDING hoặc CONFIRMED
      if (oldStatus == BookingStatus.PENDING || oldStatus == BookingStatus.CONFIRMED) {
        ServiceSlot slot =
            slotRepository
                .findByIdWithLock(booking.getSlot().getId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy slot"));
        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        slotRepository.save(slot);
      }
    } else {
      java.util.Map<BookingStatus, java.util.Set<BookingStatus>> allowed =
          java.util.Map.of(
              BookingStatus.PENDING, java.util.Set.of(BookingStatus.CONFIRMED),
              BookingStatus.CONFIRMED, java.util.Set.of(BookingStatus.CHECKED_IN),
              BookingStatus.CHECKED_IN, java.util.Set.of(BookingStatus.IN_PROGRESS),
              BookingStatus.IN_PROGRESS, java.util.Set.of(BookingStatus.COMPLETED));
      if (!allowed.getOrDefault(oldStatus, java.util.Set.of()).contains(newStatus)) {
        throw new BusinessException("Không thể chuyển trạng thái lịch hẹn theo cách này!");
      }
    }

    booking.setStatus(newStatus);
    if (note != null && !note.isBlank()) {
      booking.setNote((booking.getNote() != null ? booking.getNote() + " | " : "") + note);
    }
    booking.setUpdatedAt(LocalDateTime.now());
    Booking saved = bookingRepository.save(booking);

    eventPublisher.publishEvent(
        new BookingStatusChangedEvent(this, saved, "Cập nhật trạng thái thành " + newStatus));

    return BookingResponse.fromEntity(saved);
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> getBookingsByUser(User user) {
    return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
        .map(BookingResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> getBookingsByBranch(Long branchId) {
    return bookingRepository.findByBranchIdOrderByCreatedAtDesc(branchId).stream()
        .map(BookingResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> getAllBookings() {
    return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(BookingResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public BookingResponse getBookingDetail(Long id, User user, boolean isStaffOrAdmin) {
    Booking b =
        bookingRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy lịch đặt #" + id));

    if (!isStaffOrAdmin && !b.getUser().getId().equals(user.getId())) {
      throw new BusinessException("Bạn không có quyền xem thông tin lịch hẹn này!");
    }

    return BookingResponse.fromEntity(b);
  }

  private String generateBookingCode() {
    String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
    String randPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return "BK-" + datePart + "-" + randPart;
  }
}
