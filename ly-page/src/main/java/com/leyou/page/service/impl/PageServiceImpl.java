package com.leyou.page.service.impl;

import com.leyou.item.bo.SpuBO;
import com.leyou.item.poji.Brand;
import com.leyou.item.poji.Category;
import com.leyou.item.poji.SpecGroup;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import com.leyou.page.service.PageService;
import com.leyou.page.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PageServiceImpl implements PageService {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${ly.thymeleaf.destPath}")
    private String destPath;

    @Override
    public Map<String, Object> loadModelData(Long spuId) {
        // 模型数据
        Map<String, Object> model = new HashMap<>();
        // 查询spuBO
        SpuBO spuBO = goodsClient.querySpuBoById(spuId);
        // 将spuBo中的数据封装到model中
        // spu标题
        model.put("title", spuBO.getTitle());
        // spu副标题
        model.put("subTitle", spuBO.getSubTitle());
        // sku集合
        model.put("skus", spuBO.getSkus());
        // spuDetail
        model.put("detail", spuBO.getSpuDetail());

        // 查询品牌信息
        Brand brand = brandClient.queryBrandById(spuBO.getBrandId());
        model.put("brand", brand);

        // 查询分类信息
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spuBO.getCid1(), spuBO.getCid2(), spuBO.getCid3()));
        model.put("categories", categories);

        // 规格组和组内参数
        List<SpecGroup> specs = specClient.querySpecGroupAndParams(spuBO.getCid3());
        model.put("specs", specs);

        // 查询特有规格参数
       /* List<SpecParam> specialParams = specClient.queryParamById(null, spuBO.getCid3(), null, false);
        // 处理成 id:name格式的键值对
        Map<Long, String> paramMap = new HashMap<>();
        for (SpecParam param : specialParams) {
            paramMap.put(param.getId(), param.getName());
        }
        model.put("paramMap", paramMap);*/
        return model;
    }

    /**
     * @param id
     * @throws IOException
     */
    public void createHtml(Long id) {

        if (id == null) {
            return;
        }
        // 创建上下文
        Context context = new Context();
        // 将数据放入上下文
        context.setVariables(loadModelData(id));

        // 创建输出流，关联到一个临时文件
        File temp = new File(id + ".html");
        // 目标页面文件
        File dest = createPath(id);
        // 备份原页面文件
        File bak = new File(id + "_bak.html");
        try (PrintWriter writer = new PrintWriter(temp, "UTF-8")) {
            // 利用thymeleaf模板引擎生产静态页面
            templateEngine.process("item", context, writer);
            if (dest.exists()) {
                // 如果目标文件存在，先备份
                dest.renameTo(bak);
            }
            // 将新页面覆盖旧页面
            FileCopyUtils.copy(temp, dest);
            // 成功后将备份页面删除
            bak.delete();
        } catch (IOException e) {
            // 失败后，将备份页面恢复
            bak.renameTo(dest);
            // 记录日志
            log.error("[静态页服务] 生成静态页异常！", e);

        } finally {
            // 删除临时页面
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    /**
     * 删除已存在的页面
     *
     * @param spuId
     */
    @Override
    public void deleteHtml(Long spuId) {
        File dest = createPath(spuId);
        if (dest.exists()) {
            dest.delete();
        }
    }

    /**
     * 判断某个商品的页面是否存在
     *
     * @param spuId
     * @return
     */
    @Override
    public boolean exists(Long spuId) {
        return createPath(spuId).exists();
    }

    /**
     * 异步创建页面
     *
     * @param spuId
     */
    @Override
    public void syncCreateHtml(Long spuId) {
        ThreadUtils.execute(() -> {
            createHtml(spuId);
        });
    }

    private File createPath(Long id) {
        File dest = new File(destPath);
        if (!dest.exists()) {
            dest.mkdirs();
        }
        return new File(dest, id + ".html");
    }

}
