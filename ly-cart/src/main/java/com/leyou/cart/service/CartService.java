package com.leyou.cart.service;

import com.leyou.cart.pojo.Cart;

import java.util.List;

public interface CartService {
    void addCart(Cart cart);

    List<Cart> queryCartList();

    void updateCartNum(Long skuId, Integer num);

    void deleteCart(Long skuId);
}
