package com.ecommerce.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class SmsSender {

    private static final Logger logger = LoggerFactory.getLogger(SmsSender.class);

    @Value("${notification.twilio.account-sid:}")
    private String accountSid;

    @Value("${notification.twilio.auth-token:}")
    private String authToken;

    @Value("${notification.twilio.phone-number:}")
    private String fromPhoneNumber;

    private boolean twilioConfigured;

    @PostConstruct
    void init() {
        twilioConfigured = !accountSid.isBlank() && !authToken.isBlank() && !fromPhoneNumber.isBlank();
        if (twilioConfigured) {
            Twilio.init(accountSid, authToken);
        }
    }

    public void send(String to, String message) {
        if (!twilioConfigured) {
            logger.info("[SMS] To: {} - {}", to, message);
            return;
        }
        try {
            Message.creator(new PhoneNumber(to), new PhoneNumber(fromPhoneNumber), message).create();
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", to, e.getMessage());
        }
    }
}
