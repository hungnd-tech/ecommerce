package com.ecommerce.product.repository;

import com.ecommerce.common.repository.SoftDeleteRepository;
import com.ecommerce.product.entity.Product;

public interface ProductRepository extends SoftDeleteRepository<Product, Long> {
    
}
