package com.leyou.item.poji;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "tb_brand")
@Data
public class Brand implements Serializable {

    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private String name;// 品牌名称
    private String image;// 品牌图片
    private Character letter;

}
