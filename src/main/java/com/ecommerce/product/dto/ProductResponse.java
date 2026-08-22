package com.ecommerce.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductResponse {
    private Long id;
    private String type;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private boolean requiresShipping;
    private BigDecimal weightKg;   // null nếu là DIGITAL
    private String downloadUrl;    // null nếu là PHYSICAL
    private List<String> categories;
}
