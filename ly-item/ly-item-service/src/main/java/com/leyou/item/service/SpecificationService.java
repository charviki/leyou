package com.leyou.item.service;

import com.leyou.item.poji.SpecGroup;
import com.leyou.item.poji.SpecParam;

import java.util.List;

public interface SpecificationService {

    List<SpecGroup> queryGroupByCid(Long cid);

    List<SpecParam> queryParamById(Long gid, Long cid, Boolean searching,Boolean generic);

    List<SpecGroup> querySpecGroupAndParams(Long cid);
}
