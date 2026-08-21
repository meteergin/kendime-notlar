package com.example.order.service;

import com.example.common.events.PaymentFailedEvent;
import com.example.common.events.ShippingArrangedEvent;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "shipping-arranged-event", groupId = "order-group")
    public void consumeShippingArrangedEvent(ShippingArrangedEvent event) {
        log.info("Consumed ShippingArrangedEvent: {}", event);
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.ORDER_COMPLETED);
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = "payment-failed-event", groupId = "order-group")
    public void consumePaymentFailedEvent(PaymentFailedEvent event) {
        log.info("Consumed PaymentFailedEvent: {}", event);
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.ORDER_CANCELLED);
            orderRepository.save(order);
        });
    }
}
