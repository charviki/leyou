package com.leyou.page.service.impl;

import com.leyou.page.service.PageService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

// @RunWith(SpringRunner.class)
// @SpringBootTest
public class PageServiceImplTest {

    @Autowired
    private PageService pageService;

    @Test
    public void createHtml(){
        pageService.createHtml(141L);
    }
}
