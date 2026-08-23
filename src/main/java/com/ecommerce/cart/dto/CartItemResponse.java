package com.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CartItemResponse {

    private Long productId;
    private String productName;
    private BigDecimal unitPrice;   // giá HIỆN TẠI của product, không phải giá đóng băng như order_item
    private Integer quantity;
    private BigDecimal subtotal;    // unitPrice * quantity
    
}
