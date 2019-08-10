package com.leyou.cart.service.impl;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.UserInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.cart.service.CartService;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GoodsClient goodsClient;

    private static final String KEY_PREFIX = "cart:uid";

    /**
     * 新增购物车
     *
     * @param cart
     */
    @Override
    public void addCart(Cart cart) {

        BoundHashOperations<String, Object, Object> operations = getBoundHashOperations();

        String hashKey = cart.getSkuId().toString();
        Integer num = cart.getNum();

        // 判断当前购物车是否存在
        if (operations.hasKey(hashKey)) {
            // 存在，修改数量
            String json = operations.get(hashKey).toString();
            cart = JsonUtils.parse(json, Cart.class);
            cart.setNum(cart.getNum() + num);
        }
        // 写入redis
        operations.put(hashKey, JsonUtils.serialize(cart));
    }

    /**
     * 查询购物车
     * @return
     */
    @Override
    public List<Cart> queryCartList() {
        BoundHashOperations<String, Object, Object> operations = getBoundHashOperations();

        List<Cart> carts = operations.values().stream().map(o -> JsonUtils.parse(o.toString(), Cart.class)).collect(Collectors.toList());
        return carts;
    }

    /**
     * 获取绑定key值的hashOps
     * @return
     */
    private BoundHashOperations<String, Object, Object> getBoundHashOperations() {
        // 获取登录用户信息
        UserInfo user = UserInterceptor.getLoginUser();
        // key
        String key = KEY_PREFIX + user.getId();

        if (!redisTemplate.hasKey(key)) {
            // key不存在，返回404
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }

        return redisTemplate.boundHashOps(key);
    }

    /**
     * 更新购物车中商品数量
     * @param skuId
     * @param num
     */
    @Override
    public void updateCartNum(Long skuId, Integer num) {

        // 查询库存
        if (goodsClient.queryStockBySkuId(skuId) < num){
            // 库存不足
            throw new LyException(ExceptionEnum.STOCK_IS_NOT_ENOUGH);
        }

        BoundHashOperations<String, Object, Object> operations = getBoundHashOperations();
        String hashKey = skuId.toString();
        // 判断当前购物车是否存在
        if (!operations.hasKey(hashKey)) {
           throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        // 存在，查询购物车
        Cart cart = JsonUtils.parse(operations.get(hashKey).toString(), Cart.class);

        // 修改数量
        cart.setNum(num);
        // 写回redis
        operations.put(hashKey,JsonUtils.serialize(cart));
    }

    /**
     * 根据skuId删除购物车
     * @param skuId
     */
    @Override
    public void deleteCart(Long skuId) {
        // 获取redis操作对象
        BoundHashOperations<String, Object, Object> operations = getBoundHashOperations();
        // 删除购物车信息
        operations.delete(skuId.toString());
    }
}
