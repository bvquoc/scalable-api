package com.project.infrastructure.persistence.mapper;

import com.project.domain.model.Order;
import com.project.domain.model.OrderItem;
import com.project.infrastructure.persistence.entity.OrderEntity;
import com.project.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper to convert between OrderEntity and Order domain model.
 */
@Component
public class OrderMapper {

    /**
     * Convert OrderEntity to Order domain model.
     */
    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        Order order = new Order();
        order.setId(entity.getId());
        order.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        order.setOrderNumber(entity.getOrderNumber());
        order.setStatus(mapStatus(entity.getStatus()));
        order.setTotalAmount(entity.getTotalAmount());
        order.setShippingAddress(entity.getShippingAddress());
        order.setCreatedAt(entity.getCreatedAt());
        order.setUpdatedAt(entity.getUpdatedAt());

        // Map order items
        if (entity.getItems() != null) {
            List<OrderItem> items = entity.getItems().stream()
                    .map(this::itemToDomain)
                    .collect(Collectors.toList());
            order.setItems(items);
        }

        return order;
    }

    /**
     * Convert OrderItemEntity to OrderItem domain model.
     */
    public OrderItem itemToDomain(OrderItemEntity entity) {
        if (entity == null) {
            return null;
        }

        OrderItem item = new OrderItem();
        item.setId(entity.getId());
        item.setOrderId(entity.getOrder() != null ? entity.getOrder().getId() : null);
        item.setProductId(entity.getProduct() != null ? entity.getProduct().getId() : null);
        item.setQuantity(entity.getQuantity());
        item.setPrice(entity.getPrice());
        item.setCreatedAt(entity.getCreatedAt());

        return item;
    }

    /**
     * Convert Order domain model to OrderEntity.
     */
    public OrderEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }

        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setOrderNumber(order.getOrderNumber());
        entity.setStatus(mapStatus(order.getStatus()));
        entity.setTotalAmount(order.getTotalAmount());
        entity.setShippingAddress(order.getShippingAddress());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());

        // Map order items
        if (order.getItems() != null) {
            List<OrderItemEntity> itemEntities = order.getItems().stream()
                    .map(this::itemToEntity)
                    .collect(Collectors.toList());
            entity.setItems(itemEntities);
        }

        return entity;
    }

    /**
     * Convert OrderItem domain model to OrderItemEntity.
     */
    public OrderItemEntity itemToEntity(OrderItem item) {
        if (item == null) {
            return null;
        }

        OrderItemEntity entity = new OrderItemEntity();
        entity.setId(item.getId());
        entity.setQuantity(item.getQuantity());
        entity.setPrice(item.getPrice());
        entity.setCreatedAt(item.getCreatedAt());

        return entity;
    }

    /**
     * Update existing entity with domain model data.
     */
    public void updateEntity(OrderEntity entity, Order order) {
        if (entity == null || order == null) {
            return;
        }

        entity.setOrderNumber(order.getOrderNumber());
        entity.setStatus(mapStatus(order.getStatus()));
        entity.setTotalAmount(order.getTotalAmount());
        entity.setShippingAddress(order.getShippingAddress());
    }

    private Order.OrderStatus mapStatus(OrderEntity.OrderStatus entityStatus) {
        if (entityStatus == null) {
            return null;
        }
        return Order.OrderStatus.valueOf(entityStatus.name());
    }

    private OrderEntity.OrderStatus mapStatus(Order.OrderStatus domainStatus) {
        if (domainStatus == null) {
            return null;
        }
        return OrderEntity.OrderStatus.valueOf(domainStatus.name());
    }
}
