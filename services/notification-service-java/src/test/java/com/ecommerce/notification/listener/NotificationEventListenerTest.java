package com.ecommerce.notification.listener;

import com.ecommerce.notification.service.EmailSender;
import com.ecommerce.notification.service.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private EmailSender emailSender;

    @Mock
    private SmsSender smsSender;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(redisTemplate, emailSender, smsSender);
    }

    @Test
    void pollQueue_emptyQueue_doesNothing() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("notification_queue")).thenReturn(null);

        listener.pollQueue();

        verifyNoInteractions(emailSender, smsSender);
    }

    @Test
    void pollQueue_orderProcessedEvent_sendsEmailAndSms() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        String event = "{\"type\":\"ORDER_PROCESSED\",\"order_id\":123,\"user_email\":\"jane@example.com\","
                + "\"user_phone\":\"+15551234567\",\"recommendations\":[]}";
        when(listOperations.leftPop("notification_queue")).thenReturn(event);

        listener.pollQueue();

        verify(emailSender).send(eq("jane@example.com"), contains("123"), anyString());
        verify(smsSender).send(eq("+15551234567"), contains("123"));
    }

    @Test
    void pollQueue_unrecognizedEventType_doesNotNotify() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        String event = "{\"type\":\"ORDER_CANCELLED\",\"order_id\":123}";
        when(listOperations.leftPop("notification_queue")).thenReturn(event);

        listener.pollQueue();

        verifyNoInteractions(emailSender, smsSender);
    }

    @Test
    void pollQueue_malformedJson_doesNotThrow() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("notification_queue")).thenReturn("not-json");

        listener.pollQueue();

        verifyNoInteractions(emailSender, smsSender);
    }
}
