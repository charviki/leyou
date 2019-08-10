package com.leyou.search.pojo;


import com.leyou.common.utils.JsonUtils;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


public class SearchRequestTest {

    public static void main(String[] args) {
        SearchRequest request = new SearchRequest();
        Map<String,String> map = new HashMap<>();
        map.put("specs.CPU核数.keyword","八核");
        request.setFilter(map);
        request.setKey("手机");
        request.setPage(1);
        String s = JsonUtils.serialize(request.getFilter());
        System.out.println("s = " + s);
    }

}
