package com.leyou.item.service;

import com.leyou.common.vo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.item.poji.Sku;
import com.leyou.item.poji.SpuDetail;
import com.leyou.order.dto.CartDTO;

import java.util.List;

public interface GoodsService {
    PageResult<SpuBO> querySpuBoByPage(String key, Boolean saleable, Integer page, Integer rows);

    SpuDetail querySpuDetailBySpuId(Long spuId);

    void saveGoods(SpuBO spuBo);

    List<Sku> querySkuBySpuId(Long spuId);

    void updateGoods(SpuBO spuBo);

    SpuBO querySpuBoById(Long spuId);

    List<Sku> querySkuByIds(List<Long> ids);

    Integer queryStockBySkuId(Long skuId);

    void decreaseStock(List<CartDTO> carts);
}
