// File: src/main/java/com/utepetshop/service/event/SystemEventListener.java
package com.utepetshop.service.event;

import com.utepetshop.entity.Booking;
import com.utepetshop.entity.CustomerOrder;
import com.utepetshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemEventListener {
  private final NotificationService notificationService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBookingStatusChanged(BookingStatusChangedEvent event) {
    Booking b = event.getBooking();
    log.info("Handling BookingStatusChangedEvent for booking code: {}", b.getCode());

    String title;
    String message;
    switch (b.getStatus()) {
      case CONFIRMED -> {
        title = "Lịch đặt đã được xác nhận";
        message =
            String.format(
                "Lịch hẹn %s cho bé %s tại %s lúc %s ngày %s đã được xác nhận.",
                b.getService().getName(),
                b.getPet().getName(),
                b.getBranch().getName(),
                b.getSlot().getStartTime(),
                b.getSlot().getSlotDate());
      }
      case CHECKED_IN -> {
        title = "Thú cưng đã Check-in";
        message =
            String.format(
                "Bé %s đã check-in thành công tại chi nhánh %s.",
                b.getPet().getName(), b.getBranch().getName());
      }
      case IN_PROGRESS -> {
        title = "Dịch vụ đang thực hiện";
        message =
            String.format(
                "Bé %s đang được chăm sóc dịch vụ %s.",
                b.getPet().getName(), b.getService().getName());
      }
      case COMPLETED -> {
        title = "Dịch vụ hoàn thành";
        message =
            String.format(
                "Bé %s đã hoàn thành dịch vụ %s. Cảm ơn bạn đã tin tưởng UTEPet!",
                b.getPet().getName(), b.getService().getName());
      }
      case CANCELLED -> {
        title = "Lịch đặt đã bị hủy";
        message = String.format("Lịch hẹn mã %s đã được hủy thành công.", b.getCode());
      }
      default -> {
        title = "Cập nhật lịch đặt";
        message =
            String.format(
                "Lịch hẹn %s đã chuyển sang trạng thái %s.", b.getCode(), b.getStatus());
      }
    }

    notificationService.sendNotification(
        b.getUser(), title, message, "BOOKING_" + b.getStatus().name(), b.getId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOrderStatusChanged(OrderStatusChangedEvent event) {
    CustomerOrder o = event.getOrder();
    log.info("Handling OrderStatusChangedEvent for order id: {}", o.getId());

    String title = "Cập nhật đơn hàng #" + o.getId();
    String message =
        String.format(
            "Đơn hàng #%d của bạn đã chuyển sang trạng thái %s.", o.getId(), o.getStatus());

    notificationService.sendNotification(
        o.getCustomer(), title, message, "ORDER_" + o.getStatus().name(), o.getId());
  }
}
