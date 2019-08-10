package com.leyou.search.service;

import com.leyou.common.vo.PageResult;
import com.leyou.item.poji.Spu;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;

public interface SearchService {

    Goods buildGoods(Spu spu);

    PageResult<Goods> search(SearchRequest request);

    void createOrUpdateIndex(Long spuId);

    void deleteIndex(Long spuId);
}
