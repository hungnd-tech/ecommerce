package com.ecommerce.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
