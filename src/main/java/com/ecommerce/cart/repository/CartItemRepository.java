package com.ecommerce.cart.repository;

import com.ecommerce.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserId(Long userId);

    // nạp luôn product vào (JOIN bảng product ở đây) không cần LAZY thì biết là sẽ dùng đén, LAZY sẽ tách thành N Query
    // JOIN FETCH là JSQL (JPA SQL) an toàn thi quan hệ *->one (mỗi CartItem chỉ khớp đúng 1 Product)
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.user.id = :userId")
    List<CartItem> findByUserIdWithProduct(@Param("userId") Long userId);
}
