package com.ecommerce.order.service;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.order.dto.CheckoutRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.PhysicalProduct;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    // ---------- checkout ----------

    @Test
    void checkout_khiGioHangTrong_nem400_khongTaoOrder() {
        when(cartItemRepository.findByUserIdForUpdate(9L)).thenReturn(List.of());

        CheckoutRequest request = new CheckoutRequest();
        request.setReceiverName("A");
        request.setReceiverPhone("0900000000");
        request.setShippingAddress("HN");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.checkout(9L, request));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void checkout_khiDuHang_taoOrderThanhCong_truStockVaPublishEvent() {
        PhysicalProduct p1 = PhysicalProduct.builder().name("Ao").price(new BigDecimal("100000")).stockQuantity(10).build();
        p1.setId(1L);
        PhysicalProduct p2 = PhysicalProduct.builder().name("Quan").price(new BigDecimal("50000")).stockQuantity(5).build();
        p2.setId(2L);

        CartItem ci1 = CartItem.builder().product(p1).quantity(2).build();
        CartItem ci2 = CartItem.builder().product(p2).quantity(1).build();

        when(cartItemRepository.findByUserIdForUpdate(9L)).thenReturn(new ArrayList<>(List.of(ci1, ci2)));
        when(userRepository.getReferenceById(9L)).thenReturn(User.builder().id(9L).build());
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(p2));

        CheckoutRequest request = new CheckoutRequest();
        request.setReceiverName("Nguyen Van A");
        request.setReceiverPhone("0900000000");
        request.setShippingAddress("Ha Noi");

        OrderResponse response = orderService.checkout(9L, request);

        assertThat(p1.getStockQuantity()).isEqualTo(8);
        assertThat(p2.getStockQuantity()).isEqualTo(4);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(response.getItems()).hasSize(2);

        verify(orderRepository).save(any(Order.class));
        verify(cartItemRepository).deleteByUserId(9L);

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(9L);
        assertThat(captor.getValue().totalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    void checkout_khiKhongDuHang_nem409_khongTaoOrderKhongPublish() {
        PhysicalProduct p1 = PhysicalProduct.builder().name("Ao").price(new BigDecimal("100000")).stockQuantity(1).build();
        p1.setId(1L);
        CartItem ci1 = CartItem.builder().product(p1).quantity(5).build();

        when(cartItemRepository.findByUserIdForUpdate(9L)).thenReturn(new ArrayList<>(List.of(ci1)));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p1));

        CheckoutRequest request = new CheckoutRequest();
        request.setReceiverName("A");
        request.setReceiverPhone("0900000000");
        request.setShippingAddress("HN");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.checkout(9L, request));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(any());
        verifyNoInteractions(eventPublisher);
    }

    // ---------- cancelOrder ----------

    @Test
    void cancelOrder_khiPending_huyThanhCong_hoanLaiStock() {
        PhysicalProduct product = PhysicalProduct.builder().name("Ao").price(new BigDecimal("100000")).stockQuantity(8).build();
        product.setId(1L);

        Order order = Order.builder()
                .id(100L)
                .user(User.builder().id(9L).build())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("200000"))
                .build();
        order.addItem(OrderItem.builder().product(product).quantity(2).unitPrice(new BigDecimal("100000")).build());

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        orderService.cancelOrder(9L, 100L);

        assertThat(product.getStockQuantity()).isEqualTo(10);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_khiKhongPhaiChuDon_nem403() {
        Order order = Order.builder()
                .id(100L)
                .user(User.builder().id(9L).build())
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(999L, 100L));

        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void cancelOrder_khiKhongConPending_nem409() {
        Order order = Order.builder()
                .id(100L)
                .user(User.builder().id(9L).build())
                .status(OrderStatus.CANCELLED) // đã huỷ (hoặc PAID/SHIPPED...) từ trước
                .build();

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(9L, 100L));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void cancelOrder_khiKhongTonTai_nem404() {
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.cancelOrder(9L, 100L));
    }
}