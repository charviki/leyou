package com.leyou.item.bo;

import com.leyou.item.poji.Sku;
import com.leyou.item.poji.Spu;
import com.leyou.item.poji.SpuDetail;
import lombok.Data;

import javax.persistence.Transient;
import java.util.List;

@Data
public class SpuBO extends Spu {
    @Transient
    private String cname; // 商品分类名称
    @Transient
    private String bname; // 品牌名称
    @Transient
    SpuDetail spuDetail;// 商品详情
    @Transient
    List<Sku> skus;// sku列表
}
