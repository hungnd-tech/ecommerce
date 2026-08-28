package com.ecommerce.order.service;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdForUpdate(userId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng trống");
        }

        // QUAN TRỌNG: sắp xếp theo productId trước khi khoá lần lượt từng sản phẩm.
        // Nếu không, 2 đơn hàng khác nhau mua 2 sản phẩm giống nhau nhưng KHÁC THỨ TỰ trong giỏ
        // có thể khoá chéo nhau (A khoá X rồi chờ Y, B khoá Y rồi chờ X) -> deadlock.
        // Luôn khoá theo 1 thứ tự cố định loại bỏ hoàn toàn khả năng này.
        cartItems.sort(Comparator.comparing(ci -> ci.getProduct().getId()));

        Order order = Order.builder()
                .user(userRepository.getReferenceById(userId))
                .status(OrderStatus.PENDING)
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .shippingAddress(request.getShippingAddress())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Long productId = cartItem.getProduct().getId();

            // SELECT ... FOR UPDATE - khoá row này tới khi transaction commit/rollback
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không còn tồn tại"));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Sản phẩm \"" + product.getName() + "\" không đủ hàng (còn " + product.getStockQuantity() + ")");
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice()) // đóng băng giá tại thời điểm này
                    .build();
            order.addItem(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(total);
        orderRepository.save(order); // cascade tự lưu luôn toàn bộ OrderItem

        cartItemRepository.deleteByUserId(userId);

        eventPublisher.publishEvent(new OrderCreatedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getTotalAmount(),
                Instant.now()
        ));

        return toResponse(order);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));

        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền huỷ đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể huỷ đơn hàng đang ở trạng thái PENDING");
        }

        List<OrderItem> sortedItems = order.getItems().stream()
                .sorted(Comparator.comparing(oi -> oi.getProduct().getId()))
                .toList();

        for (OrderItem item : sortedItems) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không còn tồn tại"));
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        // không gọi save() - order và product đều đang managed trong transaction này, dirty-checking tự lo
    }

    @Transactional
    public void markAsPaid(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));

        if (order.getStatus() != OrderStatus.PENDING) {
            // đơn đã bị huỷ (hoặc xử lý bởi luồng khác) trước khi payment consumer kịp chạy -> bỏ qua, không ghi đè
            log.warn("Bỏ qua markAsPaid cho order #{} vì status hiện tại là {} (không phải PENDING)", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PAID);
    }

    @Transactional
    public void shipOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể giao hàng khi đơn đã ở trạng thái PAID");
        }

        order.setStatus(OrderStatus.SHIPPED);
    }

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể hoàn tất khi đơn đã ở trạng thái SHIPPED");
        }

        order.setStatus(OrderStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xem đơn hàng này");
        }
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(oi -> OrderItemResponse.builder()
                        .productId(oi.getProduct().getId())
                        .productName(oi.getProduct().getName())
                        .quantity(oi.getQuantity())
                        .unitPrice(oi.getUnitPrice())
                        .subtotal(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                        .build())
                .toList();

        PaymentStatus paymentStatus = paymentRepository.findByOrderId(order.getId())
                .map(Payment::getStatus)
                .orElse(null); // chưa có payment (consumer async chưa xử lý xong, hoặc đơn đã bị huỷ trước đó)

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(items)
                .paymentStatus(paymentStatus)
                .build();
    }
}