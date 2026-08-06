package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderItem;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EventPublisherService eventPublisherService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, redisTemplate, eventPublisherService);
    }

    private Order sampleOrder(Long id, Long userId, String status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setDestination("123 Main St");
        order.setWeight(2.5);
        order.setItems("[{\"product_id\":10,\"quantity\":2}]");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    @Test
    void createOrder_savesAndPublishesEvent() throws Exception {
        when(orderRepository.createOrder(org.mockito.ArgumentMatchers.any(Order.class))).thenReturn(42L);

        OrderResponse response = orderService.createOrder(1L, "123 Main St", 2.5, List.of(new OrderItem(10, 2)));

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("pending");
        verify(eventPublisherService).publishOrderCreated(42L, 1L);
    }

    @Test
    void getOrder_cacheHit_returnsWhenOwnedByRequester() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String cachedJson = "{\"id\":5,\"userId\":1,\"status\":\"pending\",\"destination\":\"123 Main St\","
                + "\"weight\":2.5,\"items\":[],\"createdAt\":\"2026-01-01T00:00:00\",\"updatedAt\":\"2026-01-01T00:00:00\"}";
        when(valueOperations.get("order:5")).thenReturn(cachedJson);

        Optional<OrderResponse> result = orderService.getOrder(5L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(5L);
        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    void getOrder_cacheHit_ownedByAnotherUser_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String cachedJson = "{\"id\":5,\"userId\":99,\"status\":\"pending\",\"destination\":\"123 Main St\","
                + "\"weight\":2.5,\"items\":[],\"createdAt\":\"2026-01-01T00:00:00\",\"updatedAt\":\"2026-01-01T00:00:00\"}";
        when(valueOperations.get("order:5")).thenReturn(cachedJson);

        Optional<OrderResponse> result = orderService.getOrder(5L, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getOrder_cacheMiss_fetchesFromRepositoryAndCaches() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:5")).thenReturn(null);
        Order order = sampleOrder(5L, 1L, "pending");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.parseItems(order.getItems())).thenReturn(List.of(new OrderItem(10, 2)));

        Optional<OrderResponse> result = orderService.getOrder(5L, 1L);

        assertThat(result).isPresent();
        verify(valueOperations).set(eq("order:5"), anyString());
    }

    @Test
    void getOrder_notFound_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:404")).thenReturn(null);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        Optional<OrderResponse> result = orderService.getOrder(404L, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getOrder_notOwnedByRequester_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:5")).thenReturn(null);
        Order order = sampleOrder(5L, 99L, "pending");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        Optional<OrderResponse> result = orderService.getOrder(5L, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    void updateOrder_validTransition_updatesStatusAndInvalidatesCache() {
        Order order = sampleOrder(5L, 1L, "pending");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.parseItems(order.getItems())).thenReturn(List.of(new OrderItem(10, 2)));

        Optional<OrderResponse> result = orderService.updateOrder(5L, 1L, "processing", null);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("processing");
        verify(orderRepository).update(order);
        verify(redisTemplate).delete("order:5");
    }

    @Test
    void updateOrder_invalidTransition_throws() {
        Order order = sampleOrder(5L, 1L, "delivered");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrder(5L, 1L, "processing", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateOrder_notOwnedByRequester_returnsEmpty() {
        Order order = sampleOrder(5L, 99L, "pending");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        Optional<OrderResponse> result = orderService.updateOrder(5L, 1L, "processing", null);

        assertThat(result).isEmpty();
        verify(orderRepository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelOrder_pendingOrder_cancelsAndPublishesEvent() {
        Order order = sampleOrder(5L, 1L, "pending");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        boolean result = orderService.cancelOrder(5L, 1L);

        assertThat(result).isTrue();
        assertThat(order.getStatus()).isEqualTo("cancelled");
        verify(redisTemplate).delete("order:5");
        verify(eventPublisherService).publishOrderCancelled(5L, 1L);
    }

    @Test
    void cancelOrder_alreadyShipped_throws() {
        Order order = sampleOrder(5L, 1L, "shipped");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(5L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventPublisherService, never()).publishOrderCancelled(anyLong(), anyLong());
    }

    @Test
    void cancelOrder_notOwnedByRequester_returnsFalse() {
        Order order = sampleOrder(5L, 99L, "pending");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        boolean result = orderService.cancelOrder(5L, 1L);

        assertThat(result).isFalse();
    }

    @Test
    void cancelOrder_notFound_returnsFalse() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        boolean result = orderService.cancelOrder(404L, 1L);

        assertThat(result).isFalse();
    }
}
