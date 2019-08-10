package com.leyou.page.mq;

import com.leyou.page.service.PageService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemListener {

    @Autowired
    private PageService pageService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "page.item.insert.queue"),
            exchange = @Exchange(
                    value = "ly.item.exchange",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC
            ),
            key = {"item.create","item.update"}
    ))
    public void listenCreateOrUpdate(Long spuId){
        if (spuId == null){
            return;
        }
        // 处理消息，创建静态页
        pageService.createHtml(spuId);

    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "page.item.delete.queue"),
            exchange = @Exchange(
                    value = "ly.item.exchange",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC
            ),
            key = {"item.delete"}
    ))
    public void listenDelete(Long spuId){
        if (spuId == null){
            return;
        }
        // 处理消息，删除静态页
        pageService.deleteHtml(spuId);

    }



}
