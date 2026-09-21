// File: src/main/java/com/utepetshop/service/event/BookingStatusChangedEvent.java
package com.utepetshop.service.event;

import com.utepetshop.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookingStatusChangedEvent extends ApplicationEvent {
  private final Booking booking;
  private final String message;

  public BookingStatusChangedEvent(Object source, Booking booking, String message) {
    super(source);
    this.booking = booking;
    this.message = message;
  }
}
