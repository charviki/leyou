package com.leyou.gateway.config;

import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;

    private PublicKey publicKey; // 公钥

    // 对象一旦实例化后就读取公钥
    @PostConstruct
    public void init() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
    }

}
