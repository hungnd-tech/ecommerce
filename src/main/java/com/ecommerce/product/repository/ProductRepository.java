package com.ecommerce.product.repository;

import com.ecommerce.common.repository.SoftDeleteRepository;
import com.ecommerce.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends SoftDeleteRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // báo cơ chế khoá
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdForUpdate(@Param("id") Long id); // For Update: khoá đén khi transaction done
}