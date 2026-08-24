package com.ecommerce.product.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // các con dùng chung 1 bảng bằng cách thêm cột product_type
@DiscriminatorColumn(name = "product_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder // @Builder thường không biết field của lớp cha, @SuperBuilder phải khai ở CẢ lớp cha lẫn lớp con
public abstract class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT") // kiểu text
    private String description;

    // precision = tổng số chữ số, scale = số chữ số sau dấu phẩy
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    // quan hệ N-N với Category qua bảng trung gian product_category
    @ManyToMany
    @JoinTable(
            name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default // báo Lombok dùng new HashSet khi builder không set.
    private Set<Category> categories = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // quyết định tính phí ship / trừ stock.
    public abstract boolean requiresShipping();
}
