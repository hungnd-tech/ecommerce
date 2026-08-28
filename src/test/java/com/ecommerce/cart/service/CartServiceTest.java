package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateQuantityRequest;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.product.entity.PhysicalProduct;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    // ---------- addToCart ----------

    @Test
    void addToCart_khiChuaCoTrongGio_taoMoiVoiUserReference() {
        PhysicalProduct product = PhysicalProduct.builder().name("Ao").price(new BigDecimal("100000")).stockQuantity(10).build();
        product.setId(1L);
        User userRef = User.builder().id(9L).build();

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductId(9L, 1L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(9L)).thenReturn(userRef);

        cartService.addToCart(9L, request);

        verify(cartItemRepository).save(argThat(item ->
                item.getProduct() == product && item.getUser() == userRef && item.getQuantity() == 2));
    }

    @Test
    void addToCart_khiDaCoTrongGio_congDonQuantity_khongTaoDongMoi() {
        PhysicalProduct product = PhysicalProduct.builder().name("Ao").price(new BigDecimal("100000")).stockQuantity(10).build();
        product.setId(1L);
        CartItem existing = CartItem.builder().id(5L).product(product).quantity(2).build();

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductId(9L, 1L)).thenReturn(Optional.of(existing));

        cartService.addToCart(9L, request);

        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addToCart_khiProductKhongTonTai_nem404() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addToCart(9L, request));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        verifyNoInteractions(cartItemRepository);
    }

    // ---------- updateQuantity ----------

    @Test
    void updateQuantity_khiTonTai_setQuantityMoi() {
        CartItem item = CartItem.builder().id(5L).quantity(2).build();
        when(cartItemRepository.findByUserIdAndProductId(9L, 1L)).thenReturn(Optional.of(item));

        UpdateQuantityRequest request = new UpdateQuantityRequest();
        request.setQuantity(7);

        cartService.updateQuantity(9L, 1L, request);

        assertThat(item.getQuantity()).isEqualTo(7);
    }

    @Test
    void updateQuantity_khiKhongCoTrongGio_nem404() {
        when(cartItemRepository.findByUserIdAndProductId(9L, 1L)).thenReturn(Optional.empty());

        UpdateQuantityRequest request = new UpdateQuantityRequest();
        request.setQuantity(7);

        assertThrows(ResponseStatusException.class, () -> cartService.updateQuantity(9L, 1L, request));
    }

    // ---------- removeItem ----------

    @Test
    void removeItem_khiTonTai_goiDeleteDungItem() {
        CartItem item = CartItem.builder().id(5L).build();
        when(cartItemRepository.findByUserIdAndProductId(9L, 1L)).thenReturn(Optional.of(item));

        cartService.removeItem(9L, 1L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeItem_khiKhongCoTrongGio_nem404_khongGoiDelete() {
        when(cartItemRepository.findByUserIdAndProductId(9L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> cartService.removeItem(9L, 1L));

        verify(cartItemRepository, never()).delete(any());
    }

    // ---------- getCart ----------

    @Test
    void getCart_tinhDungSubtotalVaTotal() {
        PhysicalProduct p1 = PhysicalProduct.builder().name("Ao").price(new BigDecimal("100000")).stockQuantity(10).build();
        p1.setId(1L);
        PhysicalProduct p2 = PhysicalProduct.builder().name("Quan").price(new BigDecimal("200000")).stockQuantity(5).build();
        p2.setId(2L);

        CartItem item1 = CartItem.builder().product(p1).quantity(2).build(); // 200.000
        CartItem item2 = CartItem.builder().product(p2).quantity(1).build(); // 200.000

        when(cartItemRepository.findByUserIdWithProduct(9L)).thenReturn(List.of(item1, item2));

        CartResponse response = cartService.getCart(9L);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("400000"));
    }
}