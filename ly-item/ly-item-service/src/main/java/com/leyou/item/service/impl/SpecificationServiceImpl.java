package com.leyou.item.service.impl;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.poji.SpecGroup;
import com.leyou.item.poji.SpecParam;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpecificationServiceImpl implements SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据id查询分组
     * @param cid
     * @return
     */
    @Override
    public List<SpecGroup> queryGroupByCid(Long cid) {
        // 设置查询条件
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        // 查询
        List<SpecGroup> specGroupList = specGroupMapper.select(specGroup);
        // 判断集合是否为空
        if (CollectionUtils.isEmpty(specGroupList)) {
            // 集合为空，抛出异常
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        // 返回结果集
        return specGroupList;
    }

    /**
     * 根据组id查询规格参数
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    @Override
    public List<SpecParam> queryParamById(Long gid, Long cid, Boolean searching,Boolean generic) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setSearching(searching);
        specParam.setGeneric(generic);
        List<SpecParam> specParams = specParamMapper.select(specParam);
        if (CollectionUtils.isEmpty(specParams)) {
            // 集合为空，抛出异常
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        return specParams;
    }

    /**
     * 根据分类id查询规格参数组及其组内参数
     * @param cid
     * @return
     */
    @Override
    public List<SpecGroup> querySpecGroupAndParams(Long cid) {
        // 查询规格组
        List<SpecGroup> specGroups = queryGroupByCid(cid);
        // 查询组内参数
        List<SpecParam> params = queryParamById(null, cid, null,null);
        // 先把规格参数变成一个map,map的key是规格组id，map的值是组下的所有参数
        Map<Long,List<SpecParam>> map = new HashMap<>();
        for (SpecParam param : params) {
            Long gid = param.getGroupId();
            if (!map.containsKey(gid)){
                // 如果这个组不存在，则新增一个list
                map.put(gid,new ArrayList<>());
            }
            map.get(gid).add(param);
        }

        // 填充param到group中
        for (SpecGroup group : specGroups) {
            group.setParams(map.get(group.getId()));
        }
        return specGroups;
    }
}
