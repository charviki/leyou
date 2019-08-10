package com.leyou.search.repository;

import com.leyou.common.vo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GoodsRepositoryTest {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    @Test
    public void testCreateIndex() {
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
    }

    @Test
    public void loadData() {
        int page = 1;
        int rows = 100;
        int size = 0;
        do {
            // 查询spu信息
            PageResult<SpuBO> result = goodsClient.querySpuBoByPage(null, true, page, rows);
            List<SpuBO> spus = result.getItems();
            if (CollectionUtils.isEmpty(spus)){
                break;
            }
            // 构建goods
            List<Goods> goods = spus.stream().map(searchService::buildGoods).collect(Collectors.toList());
            // 存入索引库
            goodsRepository.saveAll(goods);
            // 翻页
            page++;
            size = spus.size();
        }while (size == 100);
    }

    @Test
    public void testDelete(){
        goodsRepository.deleteAll();
    }

}
