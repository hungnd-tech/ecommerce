package com.ecommerce.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Generic base repository cho các entity có cột soft-delete deleted_at.
 *
 * @NoRepositoryBean bắt buộc phải có: nếu thiếu, Spring Data cố tạo bean thật cho
 * chính interface generic này (chưa xác định T là gì) lúc quét component, gây lỗi
 * khi khởi động app
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T, ID> extends JpaRepository<T, ID> {
    
    List<T> findAllByDeletedAtIsNull();

    Optional<T> findByIdAndDeletedAtIsNull(ID id);
}
