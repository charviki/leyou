package com.leyou.item.api;

import com.leyou.item.poji.SpecGroup;
import com.leyou.item.poji.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SpecificationApi {
    /**
     * 根据组id查询规格参数
     * @param gid
     * @return
     */
    @GetMapping("spec/params")
    List<SpecParam> queryParamById(
            @RequestParam(name = "gid",required = false)Long gid,
            @RequestParam(name = "cid",required = false)Long cid,
            @RequestParam(name = "searching",required = false)Boolean searching,
            @RequestParam(name = "generic",required = false)Boolean generic
    );

    /**
     * 根据分类id查询规格参数组及其组内参数
     * @param cid
     * @return
     */
    @GetMapping("spec/group")
    List<SpecGroup> querySpecGroupAndParams(@RequestParam("cid")Long cid);
}
