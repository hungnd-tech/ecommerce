package com.ecommerce.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(OrderCreatedEvent event) {
        try {
            // key = orderId -> các event cùng 1 order luôn vào cùng 1 partition, giữ đúng thứ tự
            kafkaTemplate.send(TOPIC, event.orderId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Gửi Kafka event order-created thất bại cho order #{}", event.orderId(), ex);
                        } else {
                            log.info("Đã gửi Kafka event order-created cho order #{}, partition={}, offset={}",
                                    event.orderId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            // send() có thể throw ĐỒNG BỘ ngay tại đây (vd Kafka down, không lấy được
            // metadata trong max.block.ms). Method này chạy SAU KHI DB đã commit xong
            // (gọi từ afterCommit callback) - để exception lọt ra ngoài publish() sẽ bị
            // Spring propagate ngược lên tận response HTTP, khiến client nhận 500 dù
            // order đã tạo thành công thật sự trong DB. Nguyên tắc: mọi thứ chạy sau
            // commit chỉ được phép "best-effort", không được phép làm sai lệch kết quả
            // đã trả về cho nghiệp vụ chính.
            log.error("Gửi Kafka event order-created thất bại (đồng bộ) cho order #{}", event.orderId(), ex);
        }
    }
}
