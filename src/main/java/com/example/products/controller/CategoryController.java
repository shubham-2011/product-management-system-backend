package com.example.products.controller;

import com.example.products.model.Category;
import com.example.products.model.Shop;
import com.example.products.repository.CategoryRepository;
import com.example.products.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private com.example.products.repository.ProductRepository productRepository;

    @GetMapping
    public List<Category> getAllCategories(Principal principal) {
        return categoryRepository.findByShopOwnerEmail(principal.getName());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category, Principal principal) {
        // Validate that the shop belongs to the current user
        if (category.getShop() != null) {
            Shop shop = shopRepository.findById(category.getShop().getId())
                    .orElseThrow(() -> new RuntimeException("Shop not found"));
            if (!shop.getOwner().getEmail().equals(principal.getName())) {
                throw new RuntimeException("Access denied: Shop does not belong to you");
            }
        }
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails, Principal principal) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Validate ownership via shop
        if (category.getShop() != null && !category.getShop().getOwner().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Access denied: You do not own this category");
        }
        
        category.setName(categoryDetails.getName());
        category.setShop(categoryDetails.getShop());
        
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, Principal principal) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (category.getShop() != null && !category.getShop().getOwner().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Access denied: You do not own this category");
        }

        if (productRepository.existsByCategoryId(id)) {
            throw new RuntimeException("Cannot delete category: products are assigned to this category. Please reassign or delete the products first.");
        }
        
        categoryRepository.delete(category);
        return ResponseEntity.ok().build();
    }
}
