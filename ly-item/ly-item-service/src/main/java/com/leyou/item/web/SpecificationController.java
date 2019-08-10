package com.leyou.item.web;

import com.leyou.item.poji.SpecGroup;
import com.leyou.item.poji.SpecParam;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specService;


    /**
     * 根据分类id查询分组
     * @param cid
     * @return
     */
    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupByCid(@PathVariable("cid")Long cid){
        return ResponseEntity.ok(specService.queryGroupByCid(cid));
    }

    /**
     * 根据组id查询规格参数
     * @param gid
     * @return
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> queryParamById(
            @RequestParam(name = "gid",required = false)Long gid,
            @RequestParam(name = "cid",required = false)Long cid,
            @RequestParam(name = "searching",required = false)Boolean searching,
            @RequestParam(name = "generic",required = false)Boolean generic
    ){
        return ResponseEntity.ok(specService.queryParamById(gid,cid,searching,generic));
    }

    /**
     * 根据分类id查询规格参数组及其组内参数
     * @param cid
     * @return
     */
    @GetMapping("group")
    public ResponseEntity<List<SpecGroup>> querySpecGroupAndParams(@RequestParam("cid")Long cid){
        return ResponseEntity.ok(specService.querySpecGroupAndParams(cid));
    }
}
