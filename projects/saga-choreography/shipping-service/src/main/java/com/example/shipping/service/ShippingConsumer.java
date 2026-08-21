package com.example.shipping.service;

import com.example.common.events.PaymentProcessedEvent;
import com.example.common.events.ShippingArrangedEvent;
import com.example.shipping.entity.Shipping;
import com.example.shipping.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingConsumer {

    private final ShippingRepository shippingRepository;
    private final ShippingProducer shippingProducer;

    @KafkaListener(topics = "payment-processed-event", groupId = "shipping-group")
    public void consumePaymentProcessedEvent(PaymentProcessedEvent event) {
        log.info("Consumed PaymentProcessedEvent: {}", event);

        Shipping shipping = new Shipping();
        shipping.setOrderId(event.getOrderId());
        shipping.setTrackingId(UUID.randomUUID().toString());
        shipping.setStatus("SHIPPED");
        shippingRepository.save(shipping);

        shippingProducer
                .sendShippingArrangedEvent(new ShippingArrangedEvent(event.getOrderId(), shipping.getTrackingId()));
        log.info("Shipping arranged for order: {}", event.getOrderId());
    }
}
