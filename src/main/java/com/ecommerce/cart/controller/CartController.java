package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateQuantityRequest;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
// để mô tả rõ cần đăng nhập (thực ra SecurityConfig đã có nhưng tránh sửa nhầm ở SecurityConfig)
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal CustomUserDetails principal) {
        return cartService.getCart(principal.getUser().getId());
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addToCart(@AuthenticationPrincipal CustomUserDetails principal,
                                          @Valid @RequestBody AddToCartRequest request) {
        cartService.addToCart(principal.getUser().getId(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Void> updateQuantity(@AuthenticationPrincipal CustomUserDetails principal,
                                               @PathVariable Long productId,
                                               @Valid @RequestBody UpdateQuantityRequest request) {
        cartService.updateQuantity(principal.getUser().getId(), productId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable Long productId) {
        cartService.removeItem(principal.getUser().getId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal CustomUserDetails principal) {
        cartService.clearCart(principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
