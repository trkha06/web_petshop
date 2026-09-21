// File: src/test/java/com/utepetshop/BookingConcurrencyIntegrationTest.java
package com.utepetshop;

import static org.junit.jupiter.api.Assertions.*;

import com.utepetshop.dto.BookingCreateRequest;
import com.utepetshop.dto.BookingResponse;
import com.utepetshop.entity.*;
import com.utepetshop.exception.BusinessException;
import com.utepetshop.exception.SlotFullException;
import com.utepetshop.repository.*;
import com.utepetshop.service.BookingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BookingConcurrencyIntegrationTest {

  @Autowired private BookingService bookingService;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private ServiceSlotRepository slotRepository;
  @Autowired private PetRepository petRepository;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private BranchRepository branchRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;

  private User customerA;
  private User customerB;
  private Pet petA;
  private Pet petB;
  private com.utepetshop.entity.Service serviceSpa;
  private Branch branchThuDuc;
  private ServiceSlot slotLastPlace;

  @BeforeEach
  void setup() {
    // 1. Tạo 2 khách hàng riêng biệt
    customerA = createTestUser("customera_test@utepet.vn", "Customer A");
    customerB = createTestUser("customerb_test@utepet.vn", "Customer B");

    // 2. Tạo thú cưng cho từng khách hàng
    petA = createTestPet(customerA, "Milo A");
    petB = createTestPet(customerB, "Lu B");

    // 3. Tạo chi nhánh và dịch vụ
    branchThuDuc = branchRepository.save(new Branch("UTE Pet Test Branch " + System.nanoTime(), "Võ Văn Ngân"));

    serviceSpa = new com.utepetshop.entity.Service();
    serviceSpa.setName("Test Spa Care " + System.nanoTime());
    serviceSpa.setPrice(new BigDecimal("150000"));
    serviceSpa.setDurationMinutes(60);
    serviceSpa.setActive(true);
    serviceSpa = serviceRepository.save(serviceSpa);

    // 4. Tạo slot chỉ còn ĐÚNG 1 CHỖ CUỐI CÙNG (capacity = 1, bookedCount = 0)
    slotLastPlace =
        new ServiceSlot(
            branchThuDuc,
            serviceSpa,
            LocalDate.now().plusDays(5),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
            1); // Capacity = 1
    slotLastPlace = slotRepository.save(slotLastPlace);
  }

  @Test
  @DisplayName("Kiểm thử 2 luồng đồng thời tranh chấp đặt slot cuối cùng: Đúng 1 luồng thành công, 1 luồng văng SlotFullException")
  void testConcurrentBookingForLastSlot() throws InterruptedException {
    int numberOfThreads = 2;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
    CountDownLatch startGun = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger slotFullCount = new AtomicInteger(0);
    List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

    // Chuẩn bị Request cho User A
    BookingCreateRequest reqA =
        BookingCreateRequest.builder()
            .branchId(branchThuDuc.getId())
            .serviceId(serviceSpa.getId())
            .slotId(slotLastPlace.getId())
            .petId(petA.getId())
            .note("Khách A đặt slot")
            .build();

    // Chuẩn bị Request cho User B
    BookingCreateRequest reqB =
        BookingCreateRequest.builder()
            .branchId(branchThuDuc.getId())
            .serviceId(serviceSpa.getId())
            .slotId(slotLastPlace.getId())
            .petId(petB.getId())
            .note("Khách B đặt slot")
            .build();

    // Luồng 1: Khách A
    executor.submit(
        () -> {
          readyLatch.countDown();
          try {
            startGun.await(); // Đợi hiệu lệnh xuất phát đồng thời
            bookingService.createBooking(reqA, customerA);
            successCount.incrementAndGet();
          } catch (SlotFullException e) {
            slotFullCount.incrementAndGet();
          } catch (Throwable t) {
            unexpectedErrors.add(t);
          } finally {
            doneLatch.countDown();
          }
        });

    // Luồng 2: Khách B
    executor.submit(
        () -> {
          readyLatch.countDown();
          try {
            startGun.await(); // Đợi hiệu lệnh xuất phát đồng thời
            bookingService.createBooking(reqB, customerB);
            successCount.incrementAndGet();
          } catch (SlotFullException e) {
            slotFullCount.incrementAndGet();
          } catch (Throwable t) {
            unexpectedErrors.add(t);
          } finally {
            doneLatch.countDown();
          }
        });

    // Chờ 2 luồng sẵn sàng ở vạch xuất phát
    readyLatch.await(5, TimeUnit.SECONDS);
    // Bắn súng hiệu lệnh để 2 thread gửi request gần như cùng một mili-giây
    startGun.countDown();

    // Chờ 2 thread hoàn thành
    boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertTrue(completed, "Các luồng xử lý phải hoàn thành trong thời gian cho phép");
    assertTrue(unexpectedErrors.isEmpty(), "Không được có lỗi bất ngờ ngoài dự kiến: " + unexpectedErrors);

    // KẾT QUẢ KỲ VỌNG CHỐNG OVERBOOKING:
    // 1. Đúng 1 thread thành công
    assertEquals(1, successCount.get(), "Chỉ duy nhất 1 khách hàng đặt thành công chỗ cuối cùng!");
    // 2. Đúng 1 thread bị từ chối với SlotFullException
    assertEquals(1, slotFullCount.get(), "Khách hàng đến sau phải nhận SlotFullException!");

    // 3. Kiểm tra tính toàn vẹn dữ liệu trong DB
    ServiceSlot updatedSlot = slotRepository.findById(slotLastPlace.getId()).orElseThrow();
    assertEquals(1, updatedSlot.getBookedCount(), "bookedCount của slot phải chính xác là 1, không vượt quá capacity!");
    assertEquals(1, updatedSlot.getCapacity(), "Capacity vẫn là 1");
  }

  @Test
  @DisplayName("Kiểm thử Hủy Booking: Khi hủy PENDING phải giảm bookedCount đi 1, khi CHECKED_IN không được hủy")
  void testCancelBookingRules() {
    BookingCreateRequest req =
        BookingCreateRequest.builder()
            .branchId(branchThuDuc.getId())
            .serviceId(serviceSpa.getId())
            .slotId(slotLastPlace.getId())
            .petId(petA.getId())
            .note("Test cancel")
            .build();

    BookingResponse b = bookingService.createBooking(req, customerA);
    ServiceSlot slotAfterBook = slotRepository.findById(slotLastPlace.getId()).orElseThrow();
    assertEquals(1, slotAfterBook.getBookedCount());

    // 1. Hủy booking khi đang PENDING -> Phải thành công và bookedCount giảm về 0
    BookingResponse cancelled = bookingService.cancelBooking(b.getId(), customerA, false);
    assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

    ServiceSlot slotAfterCancel = slotRepository.findById(slotLastPlace.getId()).orElseThrow();
    assertEquals(0, slotAfterCancel.getBookedCount(), "Khi hủy PENDING/CONFIRMED phải hoàn lại chỗ trống!");

    // 2. Đặt lại và chuyển sang CHECKED_IN -> Không được hủy
    BookingResponse b2 = bookingService.createBooking(req, customerA);
    bookingService.updateStatus(b2.getId(), BookingStatus.CONFIRMED, "Đã xác nhận");
    bookingService.updateStatus(b2.getId(), BookingStatus.CHECKED_IN, "Đã check in");

    assertThrows(
        BusinessException.class,
        () -> bookingService.cancelBooking(b2.getId(), customerA, false),
        "Khi lịch hẹn đã CHECKED_IN, khách hàng không được tự ý hủy!");
  }

  private User createTestUser(String email, String name) {
    return userRepository
        .findByEmail(email)
        .orElseGet(
            () -> {
              User u = new User();
              u.setEmail(email);
              u.setFullName(name);
              u.setPassword("Pass@123456");
              Role r =
                  roleRepository
                      .findByName(RoleName.CUSTOMER)
                      .orElseGet(() -> roleRepository.save(new Role(RoleName.CUSTOMER)));
              u.getRoles().add(r);
              return userRepository.save(u);
            });
  }

  private Pet createTestPet(User user, String petName) {
    Pet pet = new Pet();
    pet.setUser(user);
    pet.setName(petName);
    pet.setPetType(PetType.DOG);
    pet.setActive(true);
    return petRepository.save(pet);
  }
}
