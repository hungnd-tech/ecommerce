package com.ecommerce.config;

import com.ecommerce.product.dto.ProductResponse;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching // bật cơ chế @Cacheable/@CacheEvict cho toàn bộ app (giống @EnableMethodSecurity
public class RedisConfig {

    // Spring Boot tự dò bean kiểu RedisCacheConfiguration này để làm cấu hình mặc định
    // cho MỌI cache tạo qua @Cacheable, không cần khai tay CacheManager

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10));
    }

    // "products" (1 ProductResponse) và "productsList" (List<ProductResponse>) là 2 KIỂU khác nhau
    // -> mỗi cache name có serializer TƯỜNG MINH biết đúng kiểu của nó, không dùng chung 1 serializer generic
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        JsonMapper mapper = JsonMapper.builder().build();
        JavaType listOfProductResponse = mapper.getTypeFactory()
                .constructCollectionType(List.class, ProductResponse.class);

        var productSerializer = new JacksonJsonRedisSerializer<>(mapper, ProductResponse.class);
        var productListSerializer = new JacksonJsonRedisSerializer<List<ProductResponse>>(mapper, listOfProductResponse);

        RedisCacheConfiguration productConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(productSerializer));

        RedisCacheConfiguration productListConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(productListSerializer));

        return builder -> builder
                .withCacheConfiguration("products", productConfig)
                .withCacheConfiguration("productsList", productListConfig);
    }
}
