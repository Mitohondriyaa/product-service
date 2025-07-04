package io.github.mitohondriyaa.product.repository;

import io.github.mitohondriyaa.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

}