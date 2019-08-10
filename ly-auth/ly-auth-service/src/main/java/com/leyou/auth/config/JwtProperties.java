package com.leyou.auth.config;

import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {

    private String secret;
    private String pubKeyPath;
    private String priKeyPath;
    private int expire;

    private PublicKey publicKey; // 公钥
    private PrivateKey privateKey; // 私钥

    // 对象一旦实例化，就应该读取公钥和私钥
    @PostConstruct
    public void init() {
        try {
            // 公钥私钥不存在
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);
            if (!pubFile.exists() || !priFile.exists()){
                // 生成公钥私钥
                RsaUtils.generateKey(pubKeyPath,priKeyPath,secret);
            }
            // 读取公钥私钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        }catch (Exception e){
            log.error("初始化公钥和私钥失败!",e);
            throw new RuntimeException();
        }
    }

}
