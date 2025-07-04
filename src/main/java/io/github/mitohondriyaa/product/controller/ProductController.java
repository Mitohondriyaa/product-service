package io.github.mitohondriyaa.product.controller;

import io.github.mitohondriyaa.product.dto.ProductRequest;
import io.github.mitohondriyaa.product.dto.ProductResponse;
import io.github.mitohondriyaa.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody ProductRequest productRequest) {
        return productService.createProduct(productRequest);
    }
}