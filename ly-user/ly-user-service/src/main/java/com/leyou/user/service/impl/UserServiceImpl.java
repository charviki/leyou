package com.leyou.user.service.impl;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.service.UserService;
import com.leyou.user.utils.CodecUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:verify:phone:";

    @Override
    public boolean checkDate(String data, Integer type) {
        User user = new User();
        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }
        return userMapper.selectCount(user) == 0;
    }

    @Override
    public void sendCode(String phone) {
        // 生成key
        String key = KEY_PREFIX + phone;
        Map<String,String> msg = new HashMap<>();
        // 生产6位随机码
        String code = NumberUtils.generateCode(6);
        msg.put("phone",phone);
        msg.put("code",code);
        // 发送验证码
        amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",msg);

        // 保存验证码
        redisTemplate.opsForValue().set(key,code,5, TimeUnit.MINUTES);
    }

    /**
     * 用户注册
     * @param user
     * @param code
     */
    @Override
    public void register(User user, String code) {
        String key = KEY_PREFIX + user.getPhone();
        // 取出验证码
        String verifyCode = redisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(verifyCode) || !verifyCode.equals(code)){
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }
        user.setId(null);
        user.setCreated(new Date());
        // 生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        // 对密码进行加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        // 写入数据库
        boolean insertSuccess = userMapper.insertSelective(user) == 1;
        if (insertSuccess){
            try {
                redisTemplate.delete(key);
            }catch (Exception e){
                log.error("[用户服务] 删除缓存验证码失败,code:{}",code,e);
            }
        }else {
            throw new LyException(ExceptionEnum.USER_REGISTER_ERROR);
        }
    }

    /**
     * 根据用户名和密码查询用户
     * @param username
     * @param password
     * @return
     */
    @Override
    public User queryUserByUsernameAndPassword(String username, String password) {
        // 查询用户
        User record = new User();
        record.setUsername(username);
        User user = userMapper.selectOne(record);
        // 校验用户名和密码
        if (user == null || !StringUtils.equals(user.getPassword(),CodecUtils.md5Hex(password,user.getSalt()))){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        return user;
    }
}
