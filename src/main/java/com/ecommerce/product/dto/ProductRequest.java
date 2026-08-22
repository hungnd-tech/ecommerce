package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    @NotNull(message = "Loại sản phẩm không được để trống (PHYSICAL hoặc DIGITAL)")
    private ProductType type;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm tối đa 150 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá không được để trống")
    // inclusive: không bao gồm, ở đây price > 0, không được = 0 (minValue)
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    private String imageUrl;

    private BigDecimal weightKg;   // chỉ áp dụng khi type = PHYSICAL
    private String downloadUrl;    // chỉ áp dụng khi type = DIGITAL

    private List<Long> categoryIds;
}
