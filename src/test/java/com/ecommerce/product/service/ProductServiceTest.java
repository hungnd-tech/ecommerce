package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductType;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.PhysicalProduct;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    // ---------- getById ----------
    @Test
    void getById_khiSanPhamTonTai_traVeResponseDung() {
        PhysicalProduct product = PhysicalProduct.builder()
                .name("Ao thun")
                .description("Ao thun cotton")
                .price(new BigDecimal("150000"))
                .stockQuantity(10)
                .weightKg(new BigDecimal("0.3"))
                .build();
        product.setId(1L);

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Ao thun");
        assertThat(response.getType()).isEqualTo("PHYSICAL");
        assertThat(response.isRequiresShipping()).isTrue();
        assertThat(response.getWeightKg()).isEqualByComparingTo(new BigDecimal("0.3"));

        verify(productRepository, times(1)).findByIdAndDeletedAtIsNull(1L);
    }

    @Test
    void getById_khiSanPhamKhongTonTai_nem404() {
        when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.getById(99L));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
    }

    // ---------- create ----------

    @Test
    void create_sanPhamPhysical_khongCoCategory_luuDungVaKhongGoiCategoryRepo() {
        ProductRequest request = new ProductRequest();
        request.setType(ProductType.PHYSICAL);
        request.setName("Ban phim co");
        request.setPrice(new BigDecimal("1200000"));
        request.setStockQuantity(5);
        request.setWeightKg(new BigDecimal("1.1"));
        // categoryIds để null -> resolveCategories phải trả Set rỗng, KHÔNG gọi categoryRepository

        ProductResponse response = productService.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertThat(saved).isInstanceOf(PhysicalProduct.class);
        assertThat(((PhysicalProduct) saved).getWeightKg()).isEqualByComparingTo(new BigDecimal("1.1"));
        assertThat(saved.getCategories()).isEmpty();
        assertThat(response.getName()).isEqualTo("Ban phim co");
        assertThat(response.getType()).isEqualTo("PHYSICAL");

        verify(categoryRepository, never()).findByIdIn(anyList());
    }

    @Test
    void create_sanPhamDigital_voiCategoryHopLe_traVeDungCategories() {
        ProductRequest request = new ProductRequest();
        request.setType(ProductType.DIGITAL);
        request.setName("Ebook Java");
        request.setPrice(new BigDecimal("99000"));
        request.setStockQuantity(999);
        request.setDownloadUrl("https://example.com/ebook.pdf");
        request.setCategoryIds(List.of(1L, 2L));

        Category sach = Category.builder().id(1L).name("Sach").build();
        Category congNghe = Category.builder().id(2L).name("Cong nghe").build();
        when(categoryRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(sach, congNghe));

        ProductResponse response = productService.create(request);

        assertThat(response.getCategories()).containsExactlyInAnyOrder("Sach", "Cong nghe");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCategories()).hasSize(2);
    }

    @Test
    void create_khiCategoryIdKhongTonTai_nem400_khongGoiSave() {
        ProductRequest request = new ProductRequest();
        request.setType(ProductType.DIGITAL);
        request.setName("Ebook");
        request.setPrice(new BigDecimal("99000"));
        request.setStockQuantity(999);
        request.setCategoryIds(List.of(1L, 2L));

        // client yêu cầu 2 category nhưng DB chỉ tìm thấy 1 -> phải bị chặn
        when(categoryRepository.findByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(Category.builder().id(1L).name("Sach").build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.create(request));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        verify(productRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void update_khiDoiTypeSauKhiTao_nem400_khongGoiSaveAndFlush() {
        PhysicalProduct existing = PhysicalProduct.builder()
                .name("Cu sac")
                .price(new BigDecimal("200000"))
                .stockQuantity(20)
                .build();
        existing.setId(1L);

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));

        ProductRequest request = new ProductRequest();
        request.setType(ProductType.DIGITAL); // đổi type -> phải bị chặn
        request.setName("Cu sac");
        request.setPrice(new BigDecimal("200000"));
        request.setStockQuantity(20);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.update(1L, request));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_khiHopLe_capNhatDungFieldVaGoiSaveAndFlush() {
        PhysicalProduct existing = PhysicalProduct.builder()
                .name("Ban phim")
                .price(new BigDecimal("500000"))
                .stockQuantity(10)
                .weightKg(new BigDecimal("1.0"))
                .build();
        existing.setId(1L);

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductRequest request = new ProductRequest();
        request.setType(ProductType.PHYSICAL);
        request.setName("Ban phim co gaming");
        request.setPrice(new BigDecimal("650000"));
        request.setStockQuantity(15);
        request.setWeightKg(new BigDecimal("1.2"));

        ProductResponse response = productService.update(1L, request);

        assertThat(response.getName()).isEqualTo("Ban phim co gaming");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("650000"));
        assertThat(response.getWeightKg()).isEqualByComparingTo(new BigDecimal("1.2"));
        verify(productRepository).saveAndFlush(existing);
    }

    @Test
    void update_khiBiConflictVersion_nem409() {
        PhysicalProduct existing = PhysicalProduct.builder()
                .name("Ban phim")
                .price(new BigDecimal("500000"))
                .stockQuantity(10)
                .build();
        existing.setId(1L);

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(productRepository.saveAndFlush(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Product.class, 1L));

        ProductRequest request = new ProductRequest();
        request.setType(ProductType.PHYSICAL);
        request.setName("Ban phim moi");
        request.setPrice(new BigDecimal("550000"));
        request.setStockQuantity(8);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.update(1L, request));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
    }

    // ---------- softDelete ----------

    @Test
    void softDelete_khiTonTai_setDeletedAt() {
        PhysicalProduct product = PhysicalProduct.builder()
                .name("San pham cu")
                .price(new BigDecimal("100000"))
                .stockQuantity(1)
                .build();
        product.setId(1L);

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));

        productService.softDelete(1L);

        assertThat(product.getDeletedAt()).isNotNull();
    }

    @Test
    void softDelete_khiKhongTonTai_nem404() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> productService.softDelete(1L));
    }
}
