package com.leyou.item.mapper;

import com.leyou.item.poji.Sku;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

public interface SkuMapper extends Mapper<Sku>,InsertListMapper<Sku>, IdListMapper<Sku,Long> {

}