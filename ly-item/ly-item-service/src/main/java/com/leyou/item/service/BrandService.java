package com.leyou.item.service;

import com.leyou.common.vo.PageResult;
import com.leyou.item.poji.Brand;

import java.util.List;

public interface BrandService {
    PageResult<Brand> queryBrandByPageAndSort(Integer page, Integer rows, String sortBy, boolean desc, String key);

    void saveBrand(Brand brand, List<Long> cids);

    Brand queryBrandByBid(Long brandId);

    List<Brand> queryBrandByCid(Long cid);

    List<Brand> queryBrandByIds(List<Long> ids);
}
