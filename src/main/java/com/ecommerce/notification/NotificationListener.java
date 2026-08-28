package com.ecommerce.notification;

import com.ecommerce.order.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener {

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        // Mock gửi notification, giống cách Payment đang mock — không tích hợp email/SMS thật
        log.info("[MOCK NOTIFICATION] Gửi email xác nhận đơn hàng #{} cho user #{}, tổng tiền {}",
                event.orderId(), event.userId(), event.totalAmount());
    }
}
