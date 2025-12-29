package com.project.domain.service;

import com.project.domain.model.Product;
import com.project.infrastructure.persistence.entity.ProductEntity;
import com.project.infrastructure.persistence.mapper.ProductMapper;
import com.project.infrastructure.persistence.repository.ProductRepository;
import com.project.messaging.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Product domain operations.
 * Includes inventory management with event publishing.
 */
@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final KafkaProducer kafkaProducer;

    public ProductService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            KafkaProducer kafkaProducer) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * Create new product.
     */
    public Product createProduct(Product product) {
        log.info("Creating product: sku={}, name={}", product.getSku(), product.getName());

        // Check if SKU already exists
        if (productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException("Product with SKU already exists: " + product.getSku());
        }

        ProductEntity entity = productMapper.toEntity(product);
        ProductEntity saved = productRepository.save(entity);

        log.info("Product created successfully: id={}, sku={}", saved.getId(), saved.getSku());

        return productMapper.toDomain(saved);
    }

    /**
     * Get product by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        log.debug("Fetching product by id: {}", id);

        return productRepository.findById(id)
                .map(productMapper::toDomain);
    }

    /**
     * Get product by SKU.
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductBySku(String sku) {
        log.debug("Fetching product by sku: {}", sku);

        return productRepository.findBySku(sku)
                .map(productMapper::toDomain);
    }

    /**
     * Get all active products.
     */
    @Transactional(readOnly = true)
    public List<Product> getActiveProducts() {
        log.debug("Fetching all active products");

        return productRepository.findActiveProducts().stream()
                .map(productMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get products by category (paginated).
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        log.debug("Fetching products by category: {}", category);

        return productRepository.findByCategory(category, pageable)
                .map(productMapper::toDomain);
    }

    /**
     * Search products by name or SKU.
     */
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String searchTerm) {
        log.debug("Searching products: searchTerm={}", searchTerm);

        return productRepository.searchProducts(searchTerm).stream()
                .map(productMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get low stock products.
     */
    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(Integer threshold) {
        log.debug("Fetching low stock products: threshold={}", threshold);

        return productRepository.findLowStockProducts(threshold).stream()
                .map(productMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Update product.
     */
    public Product updateProduct(Long id, Product product) {
        log.info("Updating product: id={}", id);

        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        productMapper.updateEntity(entity, product);
        ProductEntity updated = productRepository.save(entity);

        log.info("Product updated successfully: id={}", id);

        return productMapper.toDomain(updated);
    }

    /**
     * Update product stock quantity.
     * Publishes inventory event to Kafka.
     */
    public Product updateStock(Long id, Integer newStock) {
        log.info("Updating product stock: id={}, newStock={}", id, newStock);

        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        Integer oldStock = entity.getStockQuantity();
        entity.setStockQuantity(newStock);
        ProductEntity updated = productRepository.save(entity);

        // Publish inventory event to Kafka
        kafkaProducer.publishInventoryEvent(
            updated.getId(),
            updated.getSku(),
            oldStock,
            newStock
        );

        log.info("Product stock updated: id={}, stock: {} -> {}", id, oldStock, newStock);

        return productMapper.toDomain(updated);
    }

    /**
     * Delete product.
     */
    public void deleteProduct(Long id) {
        log.info("Deleting product: id={}", id);

        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found: " + id);
        }

        productRepository.deleteById(id);

        log.info("Product deleted successfully: id={}", id);
    }
}
