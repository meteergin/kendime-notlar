package com.example.order.controller;

import com.example.common.dto.OrderRequest;
import com.example.common.events.OrderCreatedEvent;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    @PostMapping
    public Order createOrder(@RequestBody OrderRequest orderRequest) {
        Order order = new Order();
        order.setCustomerId(orderRequest.getCustomerId());
        order.setProductId(orderRequest.getProductId());
        order.setPrice(orderRequest.getPrice());
        order.setStatus(OrderStatus.ORDER_CREATED);
        order = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getCustomerId(), order.getPrice());
        orderProducer.sendOrderCreatedEvent(event);

        return order;
    }
}
