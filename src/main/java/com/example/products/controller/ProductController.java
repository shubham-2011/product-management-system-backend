package com.example.products.controller;

import com.example.products.dto.InventoryBatchReportDTO;
import com.example.products.model.InventoryBatch;
import com.example.products.model.Product;
import com.example.products.model.Shop;
import com.example.products.repository.ProductRepository;
import com.example.products.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private com.example.products.repository.InventoryBatchRepository inventoryBatchRepository;

    @GetMapping
    public List<Product> getAllProducts(Principal principal) {
        return productRepository.findByShopOwnerEmail(principal.getName());
    }

    @GetMapping("/report")
    public List<InventoryBatchReportDTO> getInventoryReport(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) String status,
            Principal principal) {
        
        List<InventoryBatch> batches = inventoryBatchRepository.findAllByOwnerEmail(principal.getName());
        java.time.LocalDate today = java.time.LocalDate.now();

        // Filter batches by purchase date if requested
        if (startDate != null || endDate != null) {
            batches = batches.stream()
                .filter(b -> {
                    if (b.getPurchase() == null || b.getPurchase().getPurchaseDate() == null) return false;
                    java.time.LocalDate pDate = b.getPurchase().getPurchaseDate().toLocalDate();
                    boolean afterStart = startDate == null || !pDate.isBefore(startDate);
                    boolean beforeEnd = endDate == null || !pDate.isAfter(endDate);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
        }

        List<InventoryBatchReportDTO> report = new ArrayList<>();
        
        for (InventoryBatch b : batches) {
            Product p = b.getProduct();
            InventoryBatchReportDTO dto = new InventoryBatchReportDTO();
            dto.setProductName(p.getName());
            dto.setProductSku(p.getSku());
            dto.setShopName(p.getShop().getName());
            dto.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "N/A");
            dto.setPurchaseDate(b.getPurchase() != null ? b.getPurchase().getPurchaseDate() : null);
            dto.setExpiryDate(b.getExpiryDate());
            dto.setCostPrice(b.getCostPrice());
            dto.setInitialQuantity(b.getQuantity());
            dto.setRemainingQuantity(b.getRemainingQuantity());
            
            double cost = b.getCostPrice() != null ? b.getCostPrice() : 0.0;
            dto.setInvestment(cost * b.getQuantity());

            // Profit calculation
            double grossProfit = 0.0;
            if (b.getSalesItems() != null) {
                grossProfit = b.getSalesItems().stream()
                    .filter(si -> {
                        if (si.getInvoice() == null || si.getInvoice().getCreatedAt() == null) return true;
                        java.time.LocalDate sDate = si.getInvoice().getCreatedAt().toLocalDate();
                        boolean afterStart = startDate == null || !sDate.isBefore(startDate);
                        boolean beforeEnd = endDate == null || !sDate.isAfter(endDate);
                        return afterStart && beforeEnd;
                    })
                    .mapToDouble(si -> si.getProfit() != null ? si.getProfit() : 0.0)
                    .sum();
            }
            
            double potentialLoss = 0.0;
            if (b.getRemainingQuantity() > 0 && b.getExpiryDate() != null && !b.getExpiryDate().isAfter(today)) {
                potentialLoss = cost * b.getRemainingQuantity();
            }

            dto.setGrossProfit(grossProfit);
            dto.setPotentialLoss(potentialLoss);
            dto.setNetProfit(grossProfit - potentialLoss);

            // ACCURATE BATCH STATUS CALCULATION
            String batchStatus = "IN_STOCK";
            int remQty = b.getRemainingQuantity() != null ? b.getRemainingQuantity() : 0;
            
            if (remQty <= 0) {
                batchStatus = "OUT_OF_STOCK";
            } else if (b.getExpiryDate() != null && !b.getExpiryDate().isAfter(today)) {
                batchStatus = "EXPIRED";
            } else if (b.getExpiryDate() != null && !b.getExpiryDate().isAfter(today.plusDays(30))) {
                batchStatus = "NEAR_EXPIRY";
            } else {
                double percentage = (double) b.getRemainingQuantity() / b.getQuantity() * 100;
                if (percentage < 30) {
                    batchStatus = "LOW_STOCK";
                }
            }
            
            dto.setStockStatus(batchStatus);
            dto.setSupplierName(b.getPurchase() != null ? b.getPurchase().getSupplier() : "N/A");
            dto.setMrp(p.getMrp());

            // --- Recommended Discount Calculation ---
            double discount = 10.0; // Base
            if (batchStatus.equals("EXPIRED")) discount += 40.0;
            else if (batchStatus.equals("NEAR_EXPIRY")) discount += 20.0;
            
            if (b.getRemainingQuantity() > 100) discount += 10.0; // Overstock
            
            dto.setRecommendedDiscount(discount);

            // Loss after discount = (Cost - (MRP * (1 - Discount%))) * RemainingQty
            double mrpVal = p.getMrp() != null ? p.getMrp() : 0.0;
            double discountedPrice = mrpVal * (1 - (discount / 100.0));
            double costPerUnit = b.getCostPrice() != null ? b.getCostPrice() : 0.0;
            int remainingQty = b.getRemainingQuantity() != null ? b.getRemainingQuantity() : 0;
            
            dto.setLossAfterDiscount((costPerUnit - discountedPrice) * remainingQty);

            // Apply status filter if provided
            if (status == null || status.isEmpty() || status.equalsIgnoreCase(batchStatus) || 
               (status.equalsIgnoreCase("EXPIRED") && batchStatus.equalsIgnoreCase("NEAR_EXPIRY"))) {
                report.add(dto);
            }
        }
        return report;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOPKEEPER')")
    public ResponseEntity<Product> createProduct(@RequestBody Product product, Principal principal) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new RuntimeException("Product name is required");
        }
        if (product.getMrp() == null || product.getMrp() <= 0) {
            throw new RuntimeException("MRP must be greater than 0");
        }
        if (product.getShop() != null) {
            Shop shop = shopRepository.findById(product.getShop().getId())
                    .orElseThrow(() -> new RuntimeException("Shop not found"));
            if (!shop.getOwner().getEmail().equals(principal.getName())) {
                throw new RuntimeException("Access denied: Shop does not belong to you");
            }
        }
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOPKEEPER')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails, Principal principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Ensure the product belongs to the user
        if (!product.getShop().getOwner().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Access denied: You do not own this product");
        }
        
        if (productDetails.getMrp() != null && productDetails.getMrp() > 0) {
            product.setMrp(productDetails.getMrp());
        }
        product.setName(productDetails.getName());
        product.setSku(productDetails.getSku());
        product.setBrand(productDetails.getBrand());
        product.setType(productDetails.getType());
        product.setCategory(productDetails.getCategory());
        product.setShop(productDetails.getShop());
        product.setActive(productDetails.isActive());
        
        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOPKEEPER')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, Principal principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (!product.getShop().getOwner().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Access denied: You do not own this product");
        }
        
        productRepository.delete(product);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/apply-discount")
    public Product applyDiscount(@PathVariable Long id, @RequestParam Double discountPercent) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Double currentMrp = p.getMrp();
        if (currentMrp != null && discountPercent > 0) {
            Double newMrp = currentMrp * (1 - (discountPercent / 100.0));
            // Round to 2 decimal places
            newMrp = Math.round(newMrp * 100.0) / 100.0;
            p.setMrp(newMrp);
            return productRepository.save(p);
        }
        return p;
    }

    @PostMapping("/batch/{batchId}/apply-discount")
    public InventoryBatch applyBatchDiscount(@PathVariable Long batchId, @RequestParam Double discountPercent) {
        InventoryBatch b = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));
        
        b.setDiscountPercent(discountPercent);
        return inventoryBatchRepository.save(b);
    }

    @PostMapping("/{id}/clear-expired")
    public void clearExpiredStock(@PathVariable Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        java.time.LocalDate today = java.time.LocalDate.now();
        List<InventoryBatch> expiredBatches = p.getInventoryBatches().stream()
                .filter(b -> b.getRemainingQuantity() > 0 && b.getExpiryDate() != null && !b.getExpiryDate().isAfter(today))
                .collect(java.util.stream.Collectors.toList());
        
        for (InventoryBatch b : expiredBatches) {
            b.setRemainingQuantity(0); // Mark as discarded
            inventoryBatchRepository.save(b);
        }
    }

    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(@PathVariable Long categoryId, Principal principal) {
        return productRepository.findByCategoryIdAndShopOwnerEmail(categoryId, principal.getName());
    }
}
