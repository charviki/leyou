package com.leyou.search.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.item.poji.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    public Goods buildGoods(Spu spu) {

        Long spuId = spu.getId();

        // 查询分类
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categories)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        List<String> categoryNames = categories.stream().map(Category::getName).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        // 搜索字段
        String all = spu.getTitle() + StringUtils.join(categoryNames, " ") + brand.getName();

        // 查询sku
        List<Sku> skus = goodsClient.querySkuBySpuId(spuId);
        if (CollectionUtils.isEmpty(skus)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        // 对sku进行处理（只取需要的字段）
        List<Map<String, Object>> skusMini = new ArrayList<>();
        // 价格集合
        Set<Long> skuPrices = new HashSet<>();
        for (Sku sku : skus) {
            Map<String, Object> skuMini = new HashMap<>();
            skuMini.put("id", sku.getId());
            skuMini.put("title", sku.getTitle());
            // 取第一张图片
            skuMini.put("image", StringUtils.substringBefore(sku.getImages(), ","));
            skuMini.put("price", sku.getPrice());
            skusMini.add(skuMini);
            // 获取sku价格，封装到集合中
            skuPrices.add(sku.getPrice());
        }

        // 查询规格参数
        List<SpecParam> params = specClient.queryParamById(null, spu.getCid3(), true,null);
        if (CollectionUtils.isEmpty(params)) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        // 查询商品详情
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spuId);
        // 获取通用规格参数
        Map<Long, String> genericSpec = JsonUtils.parseMap(spuDetail.getGenericSpec(), Long.class, String.class);
        // 获取特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });

        Map<String, Object> specs = new HashMap<>();

        for (SpecParam param : params) {
            // 规格参数名称
            String key = param.getName();
            Object value = "";
            // 判断是否是通用规格
            if (param.getGeneric()) {
                value = genericSpec.get(param.getId());
                // 判断是否是数值类型
                if (param.getNumeric()) {
                    // 处理成段
                    value = chooseSegment(value.toString(), param);
                }
            } else {
                value = specialSpec.get(param.getId());
            }
            // 存入map
            if (StringUtils.isNotBlank(value.toString())){
                specs.put(key, value);
            }
        }

        // 构建goods对象
        Goods goods = new Goods();
        goods.setId(spuId);
        goods.setBrandId(spu.getBrandId());
        goods.setSubTitle(spu.getSubTitle());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setAll(all); // 搜索字段，包含标题，分类，品牌，规格等
        goods.setPrice(skuPrices); // 所有sku价格集合
        goods.setSkus(JsonUtils.serialize(skusMini));  //  所有sku的集合的json格式
        goods.setSpecs(specs); //  所有可搜索的规格参数

        return goods;
    }

    /**
     * 搜索功能
     *
     * @param request
     * @return
     */
    @Override
    public PageResult<Goods> search(SearchRequest request) {
        int page = request.getPage() - 1;
        int size = request.getSize();
        // 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 0. 结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 1. 分页
        queryBuilder.withPageable(PageRequest.of(page, size));
        // 2. 过滤
        QueryBuilder basicQuery = CollectionUtils.isEmpty(request.getFilter()) ? QueryBuilders.matchQuery("all",request.getKey()) : buildBasicQuery(request) ;
        queryBuilder.withQuery(basicQuery);

        // 3. 聚合分类和品牌
        // 3.1 聚合分类
        String categoryAggName = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        // 3.2 聚合品牌
        String brandAggName = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 4. 查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 5. 解析结果
        // 5.1 分页结果
        long total = result.getTotalElements();
        int totalPage = result.getTotalPages();
        List<Goods> goods = result.getContent();

        // 5.2 聚合结果
        Aggregations aggs = result.getAggregations();
        List<Category> categories = parseCategoryAgg(aggs.get(categoryAggName));
        List<Brand> brands = parseBrandAgg(aggs.get(brandAggName));

        // 6. 根据商品分类判断是否需要聚合规格参数
        List<Map<String,Object>> specs = null;
        if (categories != null && categories.size() == 1){
            // 如果商品分类只有一个，根据分类条件和基本查询条件聚合
            specs = buildSpecificationAgg(categories.get(0).getId(),basicQuery);
        }


        return new SearchResult(total, totalPage, goods, categories, brands,specs);
    }

    /**
     * 创建或更新索引
     * @param spuId
     */
    @Override
    public void createOrUpdateIndex(Long spuId) {
        // 查询spuBO
        SpuBO spuBO = goodsClient.querySpuBoById(spuId);
        // 封装spu
        Spu spu = new Spu();
        BeanUtils.copyProperties(spuBO,spu);
        // 构建goods
        Goods goods = buildGoods(spu);
        // 存入索引
        repository.save(goods);
    }

    @Override
    public void deleteIndex(Long spuId) {
        repository.deleteById(spuId);
    }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.must(QueryBuilders.matchQuery("all",request.getKey()));
        // 过滤条件
        Map<String, String> filters = request.getFilter();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            // 处理key
            if( !"cid3".equals(key) && !"brandId".equals(key)){
                key = "specs." + key +".keyword";
            }
            queryBuilder.filter(QueryBuilders.termQuery(key,entry.getValue()));
        }

        return queryBuilder;
    }

    private List<Map<String, Object>> buildSpecificationAgg(Long id, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 查询可用于搜索的规格参数
        List<SpecParam> params = specClient.queryParamById(null, id, true,null);
        // 聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        params.forEach(p -> {
            String name = p.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        });
        // 查询聚合后的结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        // 解析聚合结果
        Aggregations aggs = result.getAggregations();
        params.forEach(p -> {
            String name = p.getName();
            StringTerms agg = aggs.get(name);
            // 准备map
            Map<String,Object> spec = new HashMap<>();
            spec.put("k",name);
            // .filter(b -> StringUtils.isNotBlank(b.getKeyAsString()))
            spec.put("options",agg.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList()));
            specs.add(spec);
        });
        return specs;
    }

    private List<Brand> parseBrandAgg(LongTerms terms) {
        try {
            List<Long> brandIds = terms.getBuckets().stream().map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            return brandClient.queryBrandByIds(brandIds);
        }catch (Exception e){
            log.error("【搜索服务】品牌查询失败",e);
            return null;
        }
    }

    private List<Category> parseCategoryAgg(LongTerms terms) {
        try {
            List<Long> categoryIds = terms.getBuckets().stream().map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            return categoryClient.queryCategoryByIds(categoryIds);
        }catch (Exception e){
            log.error("【查询服务】分类查询失败",e);
            return null;
        }
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

}
