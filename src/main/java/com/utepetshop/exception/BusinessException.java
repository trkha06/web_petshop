// File: src/main/java/com/utepetshop/exception/BusinessException.java
package com.utepetshop.exception;

public class BusinessException extends RuntimeException {
  public BusinessException(String message) {
    super(message);
  }
}
