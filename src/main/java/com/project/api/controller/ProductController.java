package com.project.api.controller;

import com.project.api.dto.CreateProductRequest;
import com.project.api.dto.ProductResponse;
import com.project.api.dto.UpdateStockRequest;
import com.project.domain.model.Product;
import com.project.domain.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Product operations.
 *
 * Endpoints:
 * - POST   /api/products               - Create product
 * - GET    /api/products/{id}          - Get product by ID
 * - GET    /api/products               - List active products
 * - GET    /api/products/search        - Search products
 * - GET    /api/products/low-stock     - Get low stock products
 * - PUT    /api/products/{id}          - Update product
 * - PATCH  /api/products/{id}/stock    - Update stock quantity
 * - DELETE /api/products/{id}          - Delete product
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management and inventory endpoints")
@SecurityRequirement(name = "apiKey")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Create a new product", description = "Creates a new product in the inventory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setIsActive(true);

        Product created = productService.createProduct(product);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponse.from(created));
    }

    @Operation(summary = "Get product by ID", description = "Retrieves a product by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID", required = true) @PathVariable Long id) {
        return productService.getProductById(id)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List active products", description = "Retrieves all active products, optionally filtered by category with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of active products"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getActiveProducts(
            @Parameter(description = "Filter by category (optional)") @RequestParam(required = false) String category,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        if (category != null && !category.isEmpty()) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> products = productService.getProductsByCategory(category, pageable)
                    .map(ProductResponse::from);
            return ResponseEntity.ok(products.getContent());
        }

        List<ProductResponse> products = productService.getActiveProducts().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Search products", description = "Search products by name, SKU, or description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Search query", required = true) @RequestParam String q) {
        List<ProductResponse> products = productService.searchProducts(q).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get low stock products", description = "Retrieves products with stock quantity below the specified threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of low stock products"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @Parameter(description = "Stock quantity threshold") @RequestParam(defaultValue = "10") Integer threshold) {

        List<ProductResponse> products = productService.getLowStockProducts(threshold).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Update product", description = "Updates an existing product's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request) {

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());

        Product updated = productService.updateProduct(id, product);

        return ResponseEntity.ok(ProductResponse.from(updated));
    }

    @Operation(summary = "Update product stock", description = "Updates the stock quantity of a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @Parameter(description = "Product ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request) {

        Product updated = productService.updateStock(id, request.getStockQuantity());

        return ResponseEntity.ok(ProductResponse.from(updated));
    }

    @Operation(summary = "Delete product", description = "Soft deletes a product by marking it as inactive")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
