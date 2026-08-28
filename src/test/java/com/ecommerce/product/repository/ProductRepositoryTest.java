package com.ecommerce.product.repository;

import com.ecommerce.product.entity.PhysicalProduct;
import com.ecommerce.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByIdForUpdate_khiSanPhamTonTaiVaChuaXoa_traVeDung() {
        PhysicalProduct product = PhysicalProduct.builder()
                .name("Ao thun test repo")
                .price(new BigDecimal("100000"))
                .stockQuantity(10)
                .build();
        entityManager.persistAndFlush(product);

        Optional<Product> found = productRepository.findByIdForUpdate(product.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Ao thun test repo");
    }

    @Test
    void findByIdForUpdate_khiSanPhamDaXoaMem_traVeRong() {
        PhysicalProduct product = PhysicalProduct.builder()
                .name("Ao thun da xoa test repo")
                .price(new BigDecimal("100000"))
                .stockQuantity(10)
                .build();
        product.setDeletedAt(LocalDateTime.now());
        entityManager.persistAndFlush(product);

        Optional<Product> found = productRepository.findByIdForUpdate(product.getId());

        assertThat(found).isEmpty(); // xác nhận đúng điều kiện "AND p.deletedAt IS NULL" trong JPQL
    }
}