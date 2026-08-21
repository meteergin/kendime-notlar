package com.example.payment.service;

import com.example.common.events.OrderCreatedEvent;
import com.example.common.events.PaymentFailedEvent;
import com.example.common.events.PaymentProcessedEvent;
import com.example.payment.entity.Payment;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;

    @KafkaListener(topics = "order-created-event", groupId = "payment-group")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Consumed OrderCreatedEvent: {}", event);

        // Simulate payment processing logic
        if (event.getPrice() > 1000) {
            // Fail payment if price is too high (just for demo)
            paymentProducer.sendPaymentFailedEvent(new PaymentFailedEvent(event.getOrderId(), "Price too high"));
            log.info("Payment failed for order: {}", event.getOrderId());
        } else {
            Payment payment = new Payment();
            payment.setOrderId(event.getOrderId());
            payment.setAmount(event.getPrice());
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);

            paymentProducer.sendPaymentProcessedEvent(new PaymentProcessedEvent(event.getOrderId(), event.getPrice()));
            log.info("Payment processed for order: {}", event.getOrderId());
        }
    }
}
