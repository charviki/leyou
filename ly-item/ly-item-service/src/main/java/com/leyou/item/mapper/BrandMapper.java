package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.poji.Brand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BrandMapper extends BaseMapper<Brand,Long> {

    /**
     * 向商品分类和品牌中间表插入数据
     * @param cid
     * @param bid
     */
    @Insert("insert into tb_category_brand values (#{cid},#{bid})")
    int insertCategoryAndBrand(@Param("cid") Long cid, @Param("bid") Long bid);

    /**
     * 根据分类id查询品牌信息
     * @param cid
     * @return
     */
    @Select("select * from tb_brand b left join tb_category_brand cb on b.id = cb.brand_id where cb.category_id = #{cid}")
    List<Brand> queryBrandByCid(Long cid);
}
