package com.leyou.order.dto;

import com.sun.istack.internal.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    @NotNull
    private Long addressId; // 收货地址id
    @NotNull
    private Integer paymentType; // 付款类型
    @NotNull
    private List<CartDTO> carts; // 订单详情

}
