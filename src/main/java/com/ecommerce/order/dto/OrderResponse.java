package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
