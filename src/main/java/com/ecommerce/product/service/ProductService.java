package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductType;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.DigitalProduct;
import com.ecommerce.product.entity.PhysicalProduct;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Public: Guest/Customer xem danh sách - chỉ sản phẩm chưa bị soft-delete
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAllByDeletedAtIsNull().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // @ManyToMany mặc định là LAZY, product.getCategories() chỉ gọi DB khi đến đoạn toResponse nên cần Transactional
    // readOnly: chỉ đọc data
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        return toResponse(product);
    }

    // @PreAuthorize chạy TRƯỚC khi vào method
    // Nếu không đủ quyền, ném AccessDeniedException -> trả 403,
    @PreAuthorize("hasRole('ADMIN')")
    // method chạm DB nhiều hơn 1 lần (dù chỉ đọc + ghi, hay ghi nhiều bảng) nên nằm trong đúng 1 transaction
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = buildEntity(request);
        product.setCategories(resolveCategories(request.getCategoryIds()));
        productRepository.save(product); // bảng product đã insert nhưng chưa commit
        return toResponse(product); // insert bảng product_category và commit
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product existing = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

        // Đổi loại sản phẩm sau khi đã tạo không hợp lý về nghiệp vụ
        if (!matchesType(existing, request.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đổi loại sản phẩm sau khi đã tạo");
        }

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setStockQuantity(request.getStockQuantity());
        existing.setImageUrl(request.getImageUrl());
        existing.setCategories(resolveCategories(request.getCategoryIds()));

        switch (existing) {
            case PhysicalProduct physical -> physical.setWeightKg(request.getWeightKg());
            case DigitalProduct digital -> digital.setDownloadUrl(request.getDownloadUrl());
            default -> throw new IllegalStateException("Loại sản phẩm không xác định: " + existing.getClass());
        }

        try {
            productRepository.saveAndFlush(existing);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sản phẩm vừa bị người khác cập nhật, vui lòng tải lại dữ liệu mới nhất và thử lại");
        }


        return toResponse(existing);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void softDelete(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        product.setDeletedAt(LocalDateTime.now());
    }

    private Product buildEntity(ProductRequest request) {
        return switch (request.getType()) {
            case PHYSICAL -> PhysicalProduct.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .stockQuantity(request.getStockQuantity())
                    .imageUrl(request.getImageUrl())
                    .weightKg(request.getWeightKg())
                    .build();
            case DIGITAL -> DigitalProduct.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .stockQuantity(request.getStockQuantity())
                    .imageUrl(request.getImageUrl())
                    .downloadUrl(request.getDownloadUrl())
                    .build();
        };
    }

    // kiểm tra loại sảm phầm có đung không
    private boolean matchesType(Product product, ProductType type) {
        return switch (product) {
            case PhysicalProduct p -> type == ProductType.PHYSICAL;
            case DigitalProduct d -> type == ProductType.DIGITAL;
            default -> throw new IllegalStateException("Loại sản phẩm không xác định: " + product.getClass());
        };
    }

    // kiểm tra và lấy danh sách category
    private Set<Category> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Category> found = categoryRepository.findByIdIn(categoryIds);
        if (found.size() != new HashSet<>(categoryIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có category_id không tồn tại");
        }
        return new HashSet<>(found);
    }

    // tạo rasponse
    private ProductResponse toResponse(Product product) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .requiresShipping(product.requiresShipping())
                .categories(product.getCategories().stream()
                        .map(Category::getName)
                        .toList());

        switch (product) {
            case PhysicalProduct physical -> builder.type("PHYSICAL").weightKg(physical.getWeightKg());
            case DigitalProduct digital -> builder.type("DIGITAL").downloadUrl(digital.getDownloadUrl());
            default -> throw new IllegalStateException("Loại sản phẩm không xác định: " + product.getClass());
        }

        return builder.build();
    }
}
