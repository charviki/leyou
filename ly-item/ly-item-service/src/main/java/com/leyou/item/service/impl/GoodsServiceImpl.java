package com.leyou.item.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.poji.*;
import com.leyou.item.service.BrandService;
import com.leyou.item.service.CategoryService;
import com.leyou.item.service.GoodsService;
import com.leyou.order.dto.CartDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;


    /**
     * 根据条件分页查询商品信息
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    @Override
    public PageResult<SpuBO> querySpuBoByPage(String key, Boolean saleable, Integer page, Integer rows) {
        // 分页
        PageHelper.startPage(page,rows);
        // 过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 判断查询条件是否为空
        if (StringUtils.isNotBlank(key)){
            criteria.andLike("title","%" + key + "%");
        }
        if (saleable != null){
            criteria.andEqualTo("saleable",saleable);
        }
        // 默认排序
        example.setOrderByClause("last_update_time desc");
        // 查询
        List<Spu> spuList = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(spuList)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        // 封装pageInfo对象
        PageInfo<Spu> pageInfo = new PageInfo<>(spuList);
        // 封装分类名称和品牌名称
        List<SpuBO> spuBOList = loadCategoryAndBrandName(spuList);
        // 返回结果
        return new PageResult<>(pageInfo.getTotal(), spuBOList);
    }

    /**
     * 根据spuId查询商品详情
     * @param spuId
     * @return
     */
    @Override
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null){
            throw new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return spuDetail;
    }

    /**
     * 新增商品
     * @param spuBo
     */
    @Transactional
    @Override
    public void saveGoods(SpuBO spuBo) {
        // 保存spu
        spuBo.setValid(true);
        spuBo.setSaleable(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        int count = spuMapper.insert(spuBo);
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        // 保存spuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        spuDetailMapper.insert(spuDetail);
        // 保存sku和stock
        saveSkuAndStock(spuBo);

        sendMessage(spuBo.getId(),"insert");
    }

    /**
     * 保存sku和stock
     * @param spuBo
     */
    private void saveSkuAndStock(SpuBO spuBo) {
        // 保存sku
        List<Sku> skus = spuBo.getSkus();
        List<Integer> stockNum = new ArrayList<>();
        for (Sku sku : skus) {
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            // 保存库存信息
            stockNum.add(sku.getStock());
        }

        // 批量新增sku
        int count =skuMapper.insertList(skus);
        if (count != skus.size()){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }

        // 保存stock
        List<Long> skuIds = skus.stream().map(Sku::getId).collect(Collectors.toList());
        List<Stock> stocks = new ArrayList<>();
        int time = skuIds.size();
        for (int i = 0; i < time; i++) {
            Stock stock = new Stock();
            stock.setSkuId(skuIds.get(i));
            stock.setStock(stockNum.get(i));
            stocks.add(stock);
        }
        count = stockMapper.insertList(stocks);
        if (count != skuIds.size()){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    /**
     * 根据spuId查询sku
     * @param spuId
     * @return
     */
    @Override
    public List<Sku> querySkuBySpuId(Long spuId) {
        // 设置查询条件
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        // 查询
        List<Sku> skus = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        // 取出skus集合中每个sku的id并封装到List集合中
        List<Long> skuIds = skus.stream().map(Sku::getId).collect(Collectors.toList());
        loadStockInSku(skus, skuIds);
        return skus;
    }

    private void loadStockInSku(List<Sku> skus, List<Long> skuIds) {
        // 根据id集合查询库存信息
        List<Stock> stocks = stockMapper.selectByIdList(skuIds);
        if (stocks.size() != skuIds.size()){
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        // 把库存量封装到skus集合下的每一个sku中
        /*for (int i = 0; i < stocks.size(); i++) {
            skus.get(i).setStock(stocks.get(i).getStock());
        }*/
        // 把stocks变成一个map，其key是skuId，值是库存
        Map<Long, Integer> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skus.forEach(s -> s.setStock(stockMap.get(s.getId())));
    }

    /**
     * 更新商品信息
     * (删除sku和库存，新增sku和库存，更新spu)
     * @param spuBo
     */
    @Override
    @Transactional
    public void updateGoods(SpuBO spuBo) {
        if (spuBo.getId() == null){
            throw new LyException(ExceptionEnum.GOODS_ID_CANNOT_BE_NULL);
        }
        Sku sku = new Sku();
        sku.setSpuId(spuBo.getId());
        // 查询以前的sku信息
        List<Sku> skus = skuMapper.select(sku);
        // 如果以前存在，则删除
        if (!CollectionUtils.isEmpty(skus)){
            // 先删库存，再删sku
            // 把skus集合中的每个sku的id取出来封装到list集合中
            List<Long> skuIds = skus.stream().map(Sku::getId).collect(Collectors.toList());
            // 删除以前的库存信息
            stockMapper.deleteByIdList(skuIds);
            // 删除sku,根据spuId删
            skuMapper.delete(sku);
        }

        // 修改spu
        spuBo.setValid(null);
        spuBo.setCreateTime(null);
        spuBo.setSaleable(null);
        spuBo.setLastUpdateTime(new Date());

        // 更新spu
        int count = spuMapper.updateByPrimaryKeySelective(spuBo);
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        // 更新spuDetail
        count = spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        // 新增sku和stock
        saveSkuAndStock(spuBo);

        sendMessage(spuBo.getId(),"update");
    }

    /**
     * 根据spuId查询spuBO
     * @param spuId
     * @return
     */
    @Override
    public SpuBO querySpuBoById(Long spuId) {

        SpuBO spuBO = new SpuBO();

        // 查询spu
        Spu spu =  spuMapper.selectByPrimaryKey(spuId);
        if (spu == null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        BeanUtils.copyProperties(spu,spuBO);
        // 查询spuDetail
        spuBO.setSpuDetail(querySpuDetailBySpuId(spuId));
        // 查询sku
        spuBO.setSkus(querySkuBySpuId(spuId));
        return spuBO;
    }

    /**
     * 根据skuid集合查询sku信息
     * @param ids
     * @return
     */
    @Override
    public List<Sku> querySkuByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        loadStockInSku(skus,ids);
        return skus;
    }

    /**
     * 根据skuId查询库存
     * @param skuId
     * @return
     */
    @Override
    public Integer queryStockBySkuId(Long skuId) {
        Stock stock = stockMapper.selectByPrimaryKey(skuId);
        if (stock == null){
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        return stock.getStock();
    }

    /**
     * 减库存
     * @param carts
     */
    @Override
    @Transactional
    public void decreaseStock(List<CartDTO> carts) {
        for (CartDTO cart : carts) {
            // 减库存
            int count = stockMapper.decreaseStock(cart.getSkuId(), cart.getNum());
            if (count != 1){
                throw new LyException(ExceptionEnum.STOCK_IS_NOT_ENOUGH);
            }
        }
    }

    /**
     * 根据spu对象集合加载分类名称和品牌名称
     * @param spuList
     * @return
     */
    public List<SpuBO> loadCategoryAndBrandName(List<Spu> spuList){

        List<SpuBO> spuBOList = new ArrayList<>();

        for (Spu spu : spuList) {
            // 创建SpuBo对象
            SpuBO spuBo = new SpuBO();
            // 将spu中的值封装到spuBo中
            BeanUtils.copyProperties(spu,spuBo);
            // 根据cid查询分类名称
            // 查询分类
            List<Category> categories = categoryService.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            // 获取分类名称
            List<String> categoriesName = categories.stream().map(Category::getName).collect(Collectors.toList());
            // 拼接分类名称
            String categoryNameStr = StringUtils.join(categoriesName, "/");
            // 将分类名称封装到spuBo中
            spuBo.setCname(categoryNameStr);
            // 根据bid查询品牌名称，并将品牌名称封装到spuBod对象中
            spuBo.setBname(brandService.queryBrandByBid(spu.getBrandId()).getName());

            spuBOList.add(spuBo);
        }
        return spuBOList;
    }


    private void sendMessage(Long id,String type){
        // 发送消息
        try {
            amqpTemplate.convertAndSend("item." + type,id);
        }catch (Exception e){
            log.error("{}商品消息发送异常，商品id：{}", type, id, e);
        }
    }

}
