package io.github.mitohondriyaa.product.service;

import io.github.mitohondriyaa.product.dto.ProductRequest;
import io.github.mitohondriyaa.product.dto.ProductResponse;
import io.github.mitohondriyaa.product.event.ProductCreatedEvent;
import io.github.mitohondriyaa.product.event.ProductDeletedEvent;
import io.github.mitohondriyaa.product.exception.NotFoundException;
import io.github.mitohondriyaa.product.model.Product;
import io.github.mitohondriyaa.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final RedisCacheService redisCacheService;
    private final RedisCounterService redisCounterService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${cache.threshold}")
    private Integer cacheThreshold;

    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = Product.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .build();

        productRepository.save(product);

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
        productCreatedEvent.setSkuCode(product.getName());

        kafkaTemplate.sendDefault(productCreatedEvent);

        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice());
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(product -> new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice()))
                .toList();
    }

    public ProductResponse getProductById(String id) {
        Object cached = redisCacheService.getValue(id);

        if (cached != null) {
            return (ProductResponse) cached;
        }

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        ProductResponse productResponse = new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice()
        );

        Long count = redisCounterService.incrementAndGet(id);

        if (count >= cacheThreshold) {
            redisCacheService.setValue(id, productResponse);
        }

        return productResponse;
    }

    public ProductResponse updateProductById(String id, ProductRequest productRequest) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        product.setName(productRequest.name());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());

        productRepository.save(product);

        ProductResponse productResponse = new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice()
        );

        redisCacheService.setValue(id, productResponse);
        redisCounterService.delete(id);

        return productResponse;
    }

    public void deleteProductById(String id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        productRepository.delete(product);

        redisCacheService.delete(id);
        redisCounterService.delete(id);

        ProductDeletedEvent productDeletedEvent = new ProductDeletedEvent();
        productDeletedEvent.setSkuCode(product.getName());

        kafkaTemplate.send("product-deleted", productDeletedEvent);
    }
}