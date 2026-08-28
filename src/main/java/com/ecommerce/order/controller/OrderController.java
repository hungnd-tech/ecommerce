package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public OrderResponse checkout(@AuthenticationPrincipal CustomUserDetails principal,
                                  @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(principal.getUser().getId(), request);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable Long id) {
        orderService.cancelOrder(principal.getUser().getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> ship(@PathVariable Long id) {
        orderService.shipOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> complete(@PathVariable Long id) {
        orderService.completeOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<OrderResponse> myOrders(@AuthenticationPrincipal CustomUserDetails principal) {
        return orderService.getMyOrders(principal.getUser().getId());
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return orderService.getOrder(principal.getUser().getId(), id);
    }
}
