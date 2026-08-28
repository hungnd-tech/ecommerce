package com.ecommerce.cart.repository;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.product.entity.PhysicalProduct;
import com.ecommerce.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void findByUserIdWithProduct_chiLayDungItemCuaUserDo_khongLayNhamUserKhac() {
        User userA = User.builder().email("cartrepo-a@test.com").passwordHash("x").fullName("A").role(User.Role.CUSTOMER).build();
        User userB = User.builder().email("cartrepo-b@test.com").passwordHash("x").fullName("B").role(User.Role.CUSTOMER).build();
        entityManager.persistAndFlush(userA);
        entityManager.persistAndFlush(userB);

        PhysicalProduct product = PhysicalProduct.builder().name("Ao cart repo test").price(new BigDecimal("100000")).stockQuantity(10).build();
        entityManager.persistAndFlush(product);

        entityManager.persistAndFlush(CartItem.builder().user(userA).product(product).quantity(2).build());
        entityManager.persistAndFlush(CartItem.builder().user(userB).product(product).quantity(5).build());
        entityManager.clear(); // xoá persistence context để ép query JPQL chạy thật xuống DB, không đọc nhầm từ cache

        List<CartItem> result = cartItemRepository.findByUserIdWithProduct(userA.getId());

        assertThat(result).hasSize(1); // không lẫn item của userB
        assertThat(result.get(0).getQuantity()).isEqualTo(2);
        assertThat(result.get(0).getProduct().getName()).isEqualTo("Ao cart repo test"); // xác nhận JOIN FETCH đúng
    }
}