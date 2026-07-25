package com.ecommerce.worker.service;

import com.ecommerce.worker.model.OrderItem;
import com.ecommerce.worker.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RecommendationService service;

    @Test
    void predict_withHistory_returnsAtMostFiveRecommendations() {
        List<OrderItem> history = List.of(
                new OrderItem(10, 3),
                new OrderItem(10, 2),
                new OrderItem(11, 1)
        );

        List<Integer> predictions = service.predict(history);

        assertThat(predictions).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void predict_emptyHistory_returnsDefaultRecommendations() {
        List<Integer> predictions = service.predict(List.of());

        assertThat(predictions).isNotEmpty();
    }

    @Test
    void calculateRecommendations_savesToRedisWithTtl() {
        when(orderRepository.findRecentItemsByUser(1L, 10)).thenReturn(
                List.of(new OrderItem(10, 2)));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.calculateRecommendations(1L);

        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }
}
