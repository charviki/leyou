package com.leyou.item.api;

import com.leyou.common.vo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.item.poji.Sku;
import com.leyou.item.poji.SpuDetail;
import com.leyou.order.dto.CartDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GoodsApi {

    /**
     * 根据条件分页查询商品信息
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    @GetMapping("spu/page")
    PageResult<SpuBO> querySpuBoByPage(
            @RequestParam(name = "key",required = false)String key,
            @RequestParam(name = "saleable",required = false)Boolean saleable,
            @RequestParam(name = "page",defaultValue = "1")Integer page,
            @RequestParam(name = "rows",defaultValue = "5")Integer rows
    );

    /**
     * 根据spuId查询商品详情
     * @param spuId
     * @return
     */
    @GetMapping("spu/detail/{id}")
    SpuDetail querySpuDetailBySpuId(@PathVariable("id")Long spuId);

    /**
     * 根据spuId查询sku
     * @param spuId
     * @return
     */
    @GetMapping("sku/list")
    List<Sku> querySkuBySpuId(@RequestParam("id")Long spuId);

    /**
     * 根据spuId查询spuBO
     * @param spuId
     * @return
     */
    @GetMapping("spu/{id}")
    SpuBO querySpuBoById(@PathVariable("id")Long spuId);

    /**
     * 根据skuId查询库存
     * @param skuId
     * @return
     */
    @GetMapping("stock/{skuId}")
    Integer queryStockBySkuId(@PathVariable("skuId")Long skuId);

    /**
     * 根据skuid集合查询sku信息
     * @param ids
     * @return
     */
    @GetMapping("sku/list/ids")
    List<Sku> querySkuByIds(@RequestParam("ids")List<Long> ids);

    /**
     * 减库存
     * @param carts
     * @return
     */
    @PostMapping("stock/decrease")
    void decreaseStock(@RequestBody List<CartDTO> carts);

}
