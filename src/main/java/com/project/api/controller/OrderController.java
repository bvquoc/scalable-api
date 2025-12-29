package com.project.api.controller;

import com.project.api.dto.CreateOrderRequest;
import com.project.api.dto.OrderResponse;
import com.project.domain.model.Order;
import com.project.domain.service.OrderService;
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
 * REST controller for Order operations.
 *
 * Endpoints:
 * - POST   /api/orders                    - Create order
 * - GET    /api/orders/{id}               - Get order by ID
 * - GET    /api/orders/number/{orderNum}  - Get order by order number
 * - GET    /api/orders/user/{userId}      - Get orders by user
 * - GET    /api/orders/recent             - Get recent orders
 * - PATCH  /api/orders/{id}/status        - Update order status
 * - PATCH  /api/orders/{id}/cancel        - Cancel order
 * - DELETE /api/orders/{id}               - Delete order
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management and tracking endpoints")
@SecurityRequirement(name = "apiKey")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Create a new order", description = "Creates a new order with PENDING status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setTotalAmount(request.getTotalAmount());
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(Order.OrderStatus.PENDING);

        Order created = orderService.createOrder(order);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(created));
    }

    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get order by order number", description = "Retrieves an order by its order number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(
            @Parameter(description = "Order number", required = true) @PathVariable String orderNumber) {
        return orderService.getOrderByOrderNumber(orderNumber)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get orders by user", description = "Retrieves all orders for a specific user with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of user orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orders = orderService.getOrdersByUser(userId, pageable)
                .map(OrderResponse::from);

        return ResponseEntity.ok(orders.getContent());
    }

    @Operation(summary = "Get recent orders", description = "Retrieves the most recent orders in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of recent orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<OrderResponse>> getRecentOrders() {
        List<OrderResponse> orders = orderService.getRecentOrders().stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Update order status", description = "Updates the status of an order (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id,
            @Parameter(description = "New order status", required = true) @RequestParam String status) {

        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        Order updated = orderService.updateOrderStatus(id, orderStatus);

        return ResponseEntity.ok(OrderResponse.from(updated));
    }

    @Operation(summary = "Cancel order", description = "Cancels an order by setting its status to CANCELLED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id) {
        Order cancelled = orderService.cancelOrder(id);

        return ResponseEntity.ok(OrderResponse.from(cancelled));
    }

    @Operation(summary = "Delete order", description = "Deletes an order from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
