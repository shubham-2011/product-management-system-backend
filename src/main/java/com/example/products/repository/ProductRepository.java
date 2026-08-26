package com.example.products.repository;

import com.example.products.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.inventoryBatches " +
            "WHERE p.shop.owner.email = :email")
    List<Product> findByShopOwnerEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.shop.owner.email = :email")
    List<Product> findByCategoryIdAndShopOwnerEmail(Long categoryId, String email);

    boolean existsByCategoryId(Long categoryId);
}
