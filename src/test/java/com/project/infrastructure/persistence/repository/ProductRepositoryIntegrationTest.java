package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ProductRepository.
 */
class ProductRepositoryIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProductBySku() {
        // Given
        ProductEntity product = new ProductEntity();
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setPrice(new BigDecimal("99.99"));
        product.setStockQuantity(100);
        product.setCategory("Electronics");
        product.setIsActive(true);

        // When
        ProductEntity saved = productRepository.save(product);
        Optional<ProductEntity> found = productRepository.findBySku("TEST-001");

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    void shouldFindActiveProducts() {
        // Given
        ProductEntity activeProduct = new ProductEntity();
        activeProduct.setName("Active Product");
        activeProduct.setSku("ACTIVE-001");
        activeProduct.setPrice(new BigDecimal("50.00"));
        activeProduct.setStockQuantity(10);
        activeProduct.setIsActive(true);

        ProductEntity inactiveProduct = new ProductEntity();
        inactiveProduct.setName("Inactive Product");
        inactiveProduct.setSku("INACTIVE-001");
        inactiveProduct.setPrice(new BigDecimal("30.00"));
        inactiveProduct.setStockQuantity(5);
        inactiveProduct.setIsActive(false);

        productRepository.save(activeProduct);
        productRepository.save(inactiveProduct);

        // When
        List<ProductEntity> activeProducts = productRepository.findActiveProducts();

        // Then
        assertThat(activeProducts).isNotEmpty();
        assertThat(activeProducts).allMatch(ProductEntity::getIsActive);
    }

    @Test
    void shouldFindProductsByCategory() {
        // Given
        ProductEntity electronics1 = createProduct("Electronics 1", "ELEC-001", "Electronics");
        ProductEntity electronics2 = createProduct("Electronics 2", "ELEC-002", "Electronics");
        ProductEntity clothing = createProduct("Shirt", "CLOTH-001", "Clothing");

        productRepository.save(electronics1);
        productRepository.save(electronics2);
        productRepository.save(clothing);

        // When
        Page<ProductEntity> electronicsPage = productRepository.findByCategory("Electronics", PageRequest.of(0, 10));

        // Then
        assertThat(electronicsPage.getContent()).hasSize(2);
        assertThat(electronicsPage.getContent()).allMatch(p -> p.getCategory().equals("Electronics"));
    }

    @Test
    void shouldFindLowStockProducts() {
        // Given
        ProductEntity lowStock1 = createProduct("Low Stock 1", "LOW-001", "Test");
        lowStock1.setStockQuantity(3);
        lowStock1.setIsActive(true);

        ProductEntity lowStock2 = createProduct("Low Stock 2", "LOW-002", "Test");
        lowStock2.setStockQuantity(5);
        lowStock2.setIsActive(true);

        ProductEntity highStock = createProduct("High Stock", "HIGH-001", "Test");
        highStock.setStockQuantity(100);
        highStock.setIsActive(true);

        productRepository.save(lowStock1);
        productRepository.save(lowStock2);
        productRepository.save(highStock);

        // When
        List<ProductEntity> lowStockProducts = productRepository.findLowStockProducts(10);

        // Then
        assertThat(lowStockProducts).hasSize(2);
        assertThat(lowStockProducts).allMatch(p -> p.getStockQuantity() < 10);
    }

    @Test
    void shouldSearchProductsByNameOrSku() {
        // Given
        ProductEntity product1 = createProduct("Laptop Computer", "LAP-001", "Electronics");
        ProductEntity product2 = createProduct("Desktop Computer", "DESK-001", "Electronics");
        ProductEntity product3 = createProduct("Mouse", "MOUSE-001", "Accessories");

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        // When
        List<ProductEntity> foundByName = productRepository.searchProducts("computer");
        List<ProductEntity> foundBySku = productRepository.searchProducts("LAP");

        // Then
        assertThat(foundByName).hasSize(2);
        assertThat(foundBySku).hasSize(1);
        assertThat(foundBySku.get(0).getSku()).isEqualTo("LAP-001");
    }

    @Test
    void shouldCheckSkuExists() {
        // Given
        ProductEntity product = createProduct("Exists Product", "EXISTS-001", "Test");
        productRepository.save(product);

        // When
        boolean exists = productRepository.existsBySku("EXISTS-001");
        boolean notExists = productRepository.existsBySku("NOTEXISTS-001");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    private ProductEntity createProduct(String name, String sku, String category) {
        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setSku(sku);
        product.setPrice(new BigDecimal("49.99"));
        product.setStockQuantity(20);
        product.setCategory(category);
        product.setIsActive(true);
        return product;
    }
}
