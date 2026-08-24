package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartItemResponse;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateQuantityRequest;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addToCart(Long userId, AddToCartRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"));

        var existing = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        if (existing.isPresent()) {
            // đã có trong giỏ -> cộng dồn quantity, không tạo dòng mới (tránh vi phạm UNIQUE)
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            // không cần gọi save() -> entity đang managed, Hibernate tự dirty-check khi commit
        } else {
            // getReferenceById() không chạy query, chỉ là 1 object "rỗng" chứa đúng id
            // ác field khác (email, password...) chưa được load, chỉ load thật nếu cần
            User userRef = userRepository.getReferenceById(userId);
            CartItem item = CartItem.builder()
                    .user(userRef)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(item);
        }
    }

    @Transactional
    public void updateQuantity(Long userId, Long productId, UpdateQuantityRequest request) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không có trong giỏ"));
        item.setQuantity(request.getQuantity());
    }

    @Transactional
    public void removeItem(Long userId, Long productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không có trong giỏ"));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserIdWithProduct(userId);

        List<CartItemResponse> responses = items.stream()
                .map(item -> {
                    Product p = item.getProduct(); // lazy load, OK vì còn trong transaction
                    BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return CartItemResponse.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .unitPrice(p.getPrice())
                            .quantity(item.getQuantity())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();

        BigDecimal total = responses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder().items(responses).total(total).build();
    }
}
