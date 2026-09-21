// File: src/main/java/com/utepetshop/dto/DashboardAnalyticsDto.java
package com.utepetshop.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAnalyticsDto {
  // KPI Cards
  private BigDecimal revenueToday;
  private BigDecimal revenueThisMonth;
  private long totalOrders;
  private long totalBookings;
  private long totalCustomers;
  private long totalProducts;
  private long lowStockCount;
  private long pendingBookingsCount;

  // Chart data: 7 days revenue (date -> amount)
  private List<String> revenueDates;
  private List<BigDecimal> revenueAmounts;

  // Order & Booking distribution
  private Map<String, Long> orderStatusDistribution;
  private Map<String, Long> bookingStatusDistribution;

  // Top lists
  private List<TopItemDto> topProducts;
  private List<TopItemDto> topServices;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TopItemDto {
    private Long id;
    private String name;
    private long count;
    private BigDecimal revenue;
  }
}
