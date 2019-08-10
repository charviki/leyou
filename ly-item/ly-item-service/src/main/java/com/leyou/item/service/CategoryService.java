package com.leyou.item.service;

import com.leyou.item.poji.Category;

import java.util.List;

public interface CategoryService {


    List<Category> queryCategoryByPid(Long pid) throws Exception;

    List<Category> queryCategoryByIds(List<Long> ids);
}
