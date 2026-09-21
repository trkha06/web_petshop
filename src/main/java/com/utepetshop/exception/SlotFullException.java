// File: src/main/java/com/utepetshop/exception/SlotFullException.java
package com.utepetshop.exception;

public class SlotFullException extends RuntimeException {
  public SlotFullException(String message) {
    super(message);
  }

  public SlotFullException() {
    super("Khung giờ này đã hết chỗ, vui lòng chọn khung giờ khác!");
  }
}
