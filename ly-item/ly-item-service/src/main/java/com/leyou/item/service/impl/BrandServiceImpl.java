package com.leyou.item.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.poji.Brand;
import com.leyou.item.service.BrandService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandMapper brandMapper;

    @Override
    public PageResult<Brand> queryBrandByPageAndSort(Integer page, Integer rows, String sortBy, boolean desc, String key) {
        // 分页
        PageHelper.startPage(page,rows);
        // 过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)){
            // 过滤条件
            example.createCriteria().orLike("name","%"+ key +"%").orEqualTo("letter",key.toUpperCase());
        }
        // 判断是否需要排序
        if (StringUtils.isNotBlank(sortBy)){
            // 排序
            String orderByClause = sortBy + " " + (desc ?  "desc":"asc");
            example.setOrderByClause(orderByClause);
        }
        List<Brand> brands = brandMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        PageInfo<Brand> pageInfo = new PageInfo<>(brands);
        // 查询
        return new PageResult<Brand>(pageInfo.getTotal(),brands);
    }

    @Override
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {

        // 先新增品牌
        brand.setId(null);
        int count = brandMapper.insertSelective(brand);
        if (count != 1){
            throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
        }

        // 遍历集合，向中间表中插入数据
        for (Long cid : cids) {
            count = brandMapper.insertCategoryAndBrand(cid,brand.getId());
            if (count != 1){
                throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
            }
        }

    }

    /**
     * 根据品牌id查询品牌信息
     * @param bid
     * @return
     */
    @Override
    public Brand queryBrandByBid(Long bid) {
        Brand brand = brandMapper.selectByPrimaryKey(bid);
        if (brand == null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;
    }

    /**
     * 根据分类id查询品牌信息
     * @param cid
     * @return
     */
    @Override
    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> brands = brandMapper.queryBrandByCid(cid);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }

    /**
     * 根据id集合查询品牌信息
     * @param ids
     * @return
     */
    @Override
    public List<Brand> queryBrandByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
