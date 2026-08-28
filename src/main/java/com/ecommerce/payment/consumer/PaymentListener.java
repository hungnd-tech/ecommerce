package com.ecommerce.payment.consumer;

import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // groupId RIÊNG ("payment-service") khác "notification-service" của NotificationListener -
    // cùng đọc topic order-events nhưng là 2 consumer độc lập hoàn toàn, không cần đổi gì ở KafkaConfig.
    @KafkaListener(topics = "order-events", groupId = "payment-service")
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        // Mock thanh toán: không gọi cổng thanh toán thật, luôn coi là thành công ngay lập tức
        Payment payment = Payment.builder()
                .order(orderRepository.getReferenceById(event.orderId()))
                .status(PaymentStatus.SUCCESS)
                .method("MOCK")
                .finishAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        log.info("[MOCK PAYMENT] Xử lý thanh toán order #{} thành công, số tiền {}", event.orderId(), event.totalAmount());

        orderService.markAsPaid(event.orderId());
    }
}