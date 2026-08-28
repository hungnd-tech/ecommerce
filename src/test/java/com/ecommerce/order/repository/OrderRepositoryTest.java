package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByIdForUpdate_khiTonTai_traVeDungOrder() {
        User user = User.builder().email("orderrepo-c@test.com").passwordHash("x").fullName("C").role(User.Role.CUSTOMER).build();
        entityManager.persistAndFlush(user);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100000"))
                .receiverName("A")
                .receiverPhone("0900000000")
                .shippingAddress("HN")
                .build();
        entityManager.persistAndFlush(order);

        Optional<Order> found = orderRepository.findByIdForUpdate(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByIdForUpdate_khiKhongTonTai_traVeRong() {
        assertThat(orderRepository.findByIdForUpdate(999999L)).isEmpty();
    }
}