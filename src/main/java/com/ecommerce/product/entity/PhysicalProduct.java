package com.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("PHYSICAL") // value cột product_type
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PhysicalProduct extends Product {

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Override
    public boolean requiresShipping() {
        return true;
    }
}
