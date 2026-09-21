// File: src/main/java/com/utepetshop/service/AnalyticsDashboardService.java
package com.utepetshop.service;

import com.utepetshop.dto.DashboardAnalyticsDto;
import com.utepetshop.entity.BookingStatus;
import com.utepetshop.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {
  private final OrderRepository orderRepository;
  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final InventoryRepository inventoryRepository;

  @Transactional(readOnly = true)
  public DashboardAnalyticsDto getDashboardData() {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

    // 1. Doanh thu hôm nay
    BigDecimal orderRevToday = orderRepository.sumRevenueSince(startOfDay);
    BigDecimal bookingRevToday = bookingRepository.sumRevenueSince(startOfDay);
    BigDecimal revenueToday =
        (orderRevToday != null ? orderRevToday : BigDecimal.ZERO)
            .add(bookingRevToday != null ? bookingRevToday : BigDecimal.ZERO);

    // 2. Doanh thu tháng này
    BigDecimal orderRevMonth = orderRepository.sumRevenueSince(startOfMonth);
    BigDecimal bookingRevMonth = bookingRepository.sumRevenueSince(startOfMonth);
    BigDecimal revenueThisMonth =
        (orderRevMonth != null ? orderRevMonth : BigDecimal.ZERO)
            .add(bookingRevMonth != null ? bookingRevMonth : BigDecimal.ZERO);

    // 3. Đếm số lượng
    long totalOrders = orderRepository.count();
    long totalBookings = bookingRepository.count();
    long totalCustomers = userRepository.count();
    long totalProducts = productRepository.count();
    long lowStockCount = inventoryRepository.countByQuantityLessThanEqual(5);
    long pendingBookingsCount = bookingRepository.countByStatus(BookingStatus.PENDING);

    // 4. Doanh thu 7 ngày gần nhất
    List<String> dates = new ArrayList<>();
    List<BigDecimal> amounts = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

    for (int i = 6; i >= 0; i--) {
      LocalDate d = today.minusDays(i);
      LocalDateTime dayStart = d.atStartOfDay();
      LocalDateTime dayEnd = d.atTime(LocalTime.MAX);

      BigDecimal oRev = orderRepository.sumRevenueBetween(dayStart, dayEnd);
      BigDecimal bRev = bookingRepository.sumRevenueBetween(dayStart, dayEnd);

      BigDecimal total =
          (oRev != null ? oRev : BigDecimal.ZERO)
              .add(bRev != null ? bRev : BigDecimal.ZERO);

      dates.add(d.format(formatter));
      amounts.add(total);
    }

    // 5. Tỷ lệ trạng thái đơn hàng
    Map<String, Long> orderStatusDist = new LinkedHashMap<>();
    List<Object[]> orderDistList = orderRepository.countOrdersByStatus();
    for (Object[] row : orderDistList) {
      if (row[0] != null) {
        orderStatusDist.put(row[0].toString(), (Long) row[1]);
      }
    }

    // 6. Tỷ lệ trạng thái booking
    Map<String, Long> bookingStatusDist = new LinkedHashMap<>();
    List<Object[]> bookingDistList = bookingRepository.countBookingsByStatus();
    for (Object[] row : bookingDistList) {
      if (row[0] != null) {
        bookingStatusDist.put(row[0].toString(), (Long) row[1]);
      }
    }

    // 7. Top 5 sản phẩm bán chạy
    List<DashboardAnalyticsDto.TopItemDto> topProducts = new ArrayList<>();
    List<Object[]> prodRows = orderRepository.findTopProducts();
    for (int i = 0; i < Math.min(5, prodRows.size()); i++) {
      Object[] r = prodRows.get(i);
      Long id = (Long) r[0];
      String name = (String) r[1];
      long count = ((Number) r[2]).longValue();
      BigDecimal rev = (BigDecimal) r[3];
      topProducts.add(new DashboardAnalyticsDto.TopItemDto(id, name, count, rev));
    }

    // 8. Top 5 dịch vụ được đặt nhiều nhất
    List<DashboardAnalyticsDto.TopItemDto> topServices = new ArrayList<>();
    List<Object[]> servRows = bookingRepository.findTopServices();
    for (int i = 0; i < Math.min(5, servRows.size()); i++) {
      Object[] r = servRows.get(i);
      Long id = (Long) r[0];
      String name = (String) r[1];
      long count = ((Number) r[2]).longValue();
      BigDecimal rev = (BigDecimal) r[3];
      topServices.add(new DashboardAnalyticsDto.TopItemDto(id, name, count, rev));
    }

    return DashboardAnalyticsDto.builder()
        .revenueToday(revenueToday)
        .revenueThisMonth(revenueThisMonth)
        .totalOrders(totalOrders)
        .totalBookings(totalBookings)
        .totalCustomers(totalCustomers)
        .totalProducts(totalProducts)
        .lowStockCount(lowStockCount)
        .pendingBookingsCount(pendingBookingsCount)
        .revenueDates(dates)
        .revenueAmounts(amounts)
        .orderStatusDistribution(orderStatusDist)
        .bookingStatusDistribution(bookingStatusDist)
        .topProducts(topProducts)
        .topServices(topServices)
        .build();
  }
}
