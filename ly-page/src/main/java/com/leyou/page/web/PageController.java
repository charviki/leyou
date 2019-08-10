package com.leyou.page.web;

import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("item")
public class PageController {

    @Autowired
    private PageService pageService;

    /**
     * 根据spuId查询商品详情页显示所需的数据
     * @param spuId
     * @param model
     * @return
     */
    @GetMapping("{id}.html")
    public String buildItemPage(@PathVariable("id")Long spuId, Model model){
        // 查询模型数据
        Map<String,Object> attributes = pageService.loadModelData(spuId);
        // 准备模型数据
        model.addAllAttributes(attributes);

        // 判断是否需要生产新的页面
        if(!pageService.exists(spuId)){
            pageService.syncCreateHtml(spuId);
        }

        // 返回视图
        return "item";
    }



}
