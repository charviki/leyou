package com.leyou.order.web;

import com.leyou.order.dto.OrderDTO;
import com.leyou.order.pojo.Order;
import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * @param orderDTO
     * @return
     */
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody OrderDTO orderDTO){
        return ResponseEntity.ok(orderService.createOrder(orderDTO));
    }

    /**
     * 根据订单id查询订单信息
     * @param orderId
     * @return
     */
    @GetMapping("{id}")
    public ResponseEntity<Order> queryOrderById(@PathVariable("id")Long orderId){
        return ResponseEntity.ok(orderService.queryOrderById(orderId));
    }

    /**
     * 生成微信支付二维码url
     * @param orderId
     * @return
     */
    @GetMapping("url/{id}")
    public ResponseEntity<String> createWXPayUrl(@PathVariable("id")Long orderId){
        return ResponseEntity.ok(orderService.createWXPayUrl(orderId));
    }

    /**
     * 根据订单id查询订单支付状态
     * @param orderId
     * @return
     */
    @GetMapping("state/{id}")
    public ResponseEntity<Integer> queryOrderStatusByOrderId(@PathVariable("id")Long orderId){
        return ResponseEntity.ok(orderService.queryOrderStatusByOrderId(orderId).getValue());
    }

}
