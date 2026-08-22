package com.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("DIGITAL") // value cột product_type
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DigitalProduct extends Product {

    @Column(name = "download_url")
    private String downloadUrl;

    @Override
    public boolean requiresShipping() {
        return false;
    }
}
