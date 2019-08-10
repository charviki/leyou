package com.leyou.sms.mq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SmsListenerTest {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testSend() throws InterruptedException {
        Map<String,String> msg = new HashMap<>();
        msg.put("phone","15907591408");
        msg.put("code","666666");
        amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",msg);
        Thread.sleep(10000);
    }

    @Test
    public void testRedis(){
        redisTemplate.opsForValue().set("name","charmless");
    }

}
