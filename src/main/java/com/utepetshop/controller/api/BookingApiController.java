// File: src/main/java/com/utepetshop/controller/api/BookingApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.BookingCreateRequest;
import com.utepetshop.dto.BookingResponse;
import com.utepetshop.entity.User;
import com.utepetshop.service.BookingService;
import com.utepetshop.service.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingApiController {
  private final BookingService bookingService;
  private final CurrentUser currentUser;

  @GetMapping
  public ResponseEntity<List<BookingResponse>> getMyBookings() {
    User user = currentUser.required();
    return ResponseEntity.ok(bookingService.getBookingsByUser(user));
  }

  @PostMapping
  public ResponseEntity<BookingResponse> createBooking(
      @Valid @RequestBody BookingCreateRequest request) {
    User user = currentUser.required();
    BookingResponse response = bookingService.createBooking(request, user);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BookingResponse> getBookingDetail(@PathVariable Long id) {
    User user = currentUser.required();
    return ResponseEntity.ok(bookingService.getBookingDetail(id, user, false));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
    User user = currentUser.required();
    return ResponseEntity.ok(bookingService.cancelBooking(id, user, false));
  }
}
