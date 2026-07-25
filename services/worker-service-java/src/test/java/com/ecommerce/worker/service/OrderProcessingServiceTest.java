package com.ecommerce.worker.service;

import com.ecommerce.worker.model.Order;
import com.ecommerce.worker.model.OrderItem;
import com.ecommerce.worker.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private OrderProcessingService service;

    @Test
    void processOrder_success_updatesStatusAndPublishesEvent() {
        Order order = new Order(1L, 1L, "pending", "123 Main St", 2.5,
                List.of(new OrderItem(10, 2)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.processOrder(1L);

        verify(orderRepository).updateStatus(1L, "PROCESSING");
        verify(notificationPublisher).publishEvent(eq("ORDER_PROCESSED"), anyMap());
    }

    @Test
    void processOrder_orderNotFound_doesNothing() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        service.processOrder(999L);

        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
        verify(notificationPublisher, never()).publishEvent(anyString(), anyMap());
    }

    @Test
    void processOrder_outOfStock_sendsNotificationAndSkipsStatusUpdate() {
        Order order = new Order(1L, 1L, "pending", "123 Main St", 2.5,
                List.of(new OrderItem(999, 100)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.processOrder(1L);

        verify(notificationPublisher).sendOutOfStockNotification(eq(1L), contains("999"));
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }
}
