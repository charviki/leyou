package com.leyou.item.service.impl;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.poji.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据品牌id查询商品分类信息
     * @param pid
     * @return
     * @throws Exception
     */
    @Override
    public List<Category> queryCategoryByPid(Long pid) throws Exception {
        // 查询条件，以对象中的非空属性为条件查询
        Category category = new Category();
        category.setParentId(pid);
        // 执行查询
        List<Category> categories = categoryMapper.select(category);
        // 判断结果集是否为空
        if (CollectionUtils.isEmpty(categories)){
            // 结果集为空，抛出异常
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        // 返回结果集
        return categories;
    }

    /**
     * 根据商品分类id查询商品分类信息
     * @param ids
     * @return
     */
    @Override
    public List<Category> queryCategoryByIds(List<Long> ids) {

        List<Category> categoryList = categoryMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(categoryList)){
            // 结果集为空，抛出异常
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return categoryList;
    }
}
