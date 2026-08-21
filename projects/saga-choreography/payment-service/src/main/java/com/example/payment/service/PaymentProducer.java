package com.example.payment.service;

import com.example.common.events.PaymentFailedEvent;
import com.example.common.events.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentProcessedEvent(PaymentProcessedEvent event) {
        kafkaTemplate.send("payment-processed-event", event);
    }

    public void sendPaymentFailedEvent(PaymentFailedEvent event) {
        kafkaTemplate.send("payment-failed-event", event);
    }
}
