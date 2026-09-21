// File: src/main/java/com/utepetshop/service/event/OrderStatusChangedEvent.java
package com.utepetshop.service.event;

import com.utepetshop.entity.CustomerOrder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderStatusChangedEvent extends ApplicationEvent {
  private final CustomerOrder order;
  private final String message;

  public OrderStatusChangedEvent(Object source, CustomerOrder order, String message) {
    super(source);
    this.order = order;
    this.message = message;
  }
}
