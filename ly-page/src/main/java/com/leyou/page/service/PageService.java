package com.leyou.page.service;

import java.util.Map;

public interface PageService {
    Map<String, Object> loadModelData(Long spuId);
    void createHtml(Long spuId);

    void deleteHtml(Long spuId);

    boolean exists(Long spuId);

    void syncCreateHtml(Long spuId);
}
