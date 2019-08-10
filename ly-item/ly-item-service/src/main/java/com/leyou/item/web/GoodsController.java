package com.leyou.item.web;

import com.leyou.common.vo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.item.poji.Sku;
import com.leyou.item.poji.SpuDetail;
import com.leyou.item.service.GoodsService;
import com.leyou.order.dto.CartDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 根据条件分页查询商品信息
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    @GetMapping("spu/page")
    public ResponseEntity<PageResult<SpuBO>> querySpuBoByPage(
            @RequestParam(name = "key",required = false)String key,
            @RequestParam(name = "saleable",required = false)Boolean saleable,
            @RequestParam(name = "page",defaultValue = "1")Integer page,
            @RequestParam(name = "rows",defaultValue = "5")Integer rows
    ){
        return ResponseEntity.ok(goodsService.querySpuBoByPage(key,saleable,page,rows));
    }

    /**
     * 根据spuId查询商品详情
     * @param spuId
     * @return
     */
    @GetMapping("spu/detail/{id}")
    public ResponseEntity<SpuDetail> querySpuDetailBySpuId(@PathVariable("id")Long spuId){
        return ResponseEntity.ok(goodsService.querySpuDetailBySpuId(spuId));
    }

    /**
     * 新增商品
     * @param spuBo
     * @return
     */
    @PostMapping("goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuBO spuBo){
        goodsService.saveGoods(spuBo);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据spuId查询sku
     * @param spuId
     * @return
     */
    @GetMapping("sku/list")
    public ResponseEntity<List<Sku>> querySkuBySpuId(@RequestParam("id")Long spuId){
        return ResponseEntity.ok(goodsService.querySkuBySpuId(spuId));
    }

    /**
     * 更新商品信息
     * @param spuBo
     * @return
     */
    @PutMapping("goods")
    public ResponseEntity<Void> updateGoods(@RequestBody SpuBO spuBo){
        goodsService.updateGoods(spuBo);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 根据spuId查询spuBO
     * @param spuId
     * @return
     */
    @GetMapping("spu/{id}")
    public ResponseEntity<SpuBO> querySpuBoById(@PathVariable("id")Long spuId){
        return ResponseEntity.ok(goodsService.querySpuBoById(spuId));
    }

    /**
     * 根据skuid集合查询sku信息
     * @param ids
     * @return
     */
    @GetMapping("sku/list/ids")
    public ResponseEntity<List<Sku>> querySkuByIds(@RequestParam("ids")List<Long> ids){
        return ResponseEntity.ok(goodsService.querySkuByIds(ids));
    }

    /**
     * 根据skuId查询库存
     * @param skuId
     * @return
     */
    @GetMapping("stock/{skuId}")
    public ResponseEntity<Integer> queryStockBySkuId(@PathVariable("skuId")Long skuId){
        return ResponseEntity.ok(goodsService.queryStockBySkuId(skuId));
    }

    /**
     * 减库存
     * @param carts
     * @return
     */
    @PostMapping("stock/decrease")
    public ResponseEntity<Void> decreaseStock(@RequestBody List<CartDTO> carts){
        goodsService.decreaseStock(carts);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
