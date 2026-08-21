package com.example.shipping.service;

import com.example.common.events.ShippingArrangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShippingProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendShippingArrangedEvent(ShippingArrangedEvent event) {
        kafkaTemplate.send("shipping-arranged-event", event);
    }
}
