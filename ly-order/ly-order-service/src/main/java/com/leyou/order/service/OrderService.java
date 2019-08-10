package com.leyou.order.service;

import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.PayState;
import com.leyou.order.pojo.Order;

import java.util.Map;

public interface OrderService {
    Long createOrder(OrderDTO orderDTO);

    Order queryOrderById(Long orderId);

    String createWXPayUrl(Long orderId);

    void handleNotify(Map<String, String> result);

    PayState queryOrderStatusByOrderId(Long orderId);
}
