package com.lilac.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权配置：绑定 application.yml 中的 glean.auth.*
 * <p>
 * 不使用校验注解，长度检查在 {@link #secretBytes()} 里手写。
 * 校验失败会抛 IllegalStateException，让应用启动失败——
 * 宁可起不来，也不要带着弱密钥跑在线上。
 */
@Data
@ConfigurationProperties(prefix = "glean.auth")
public class AuthProperties {

    /**
     * HS256 密钥，至少 32 字节（256 bit）
     */
    private String jwtSecret;

    /**
     * token 有效期（秒），默认 2 小时
     */
    private int tokenExpireSeconds = 7200;

    /**
     * 签发者标识
     */
    private String issuer = "glean";

    /**
     * 密钥字节，同时完成校验
     *
     * @throws IllegalStateException 密钥未配置或不足 32 字节
     */
    public byte[] secretBytes() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("glean.auth.jwt-secret 未配置，请检查 application-dev.yml");
        }
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "glean.auth.jwt-secret 至少需要 32 字节，当前 " + bytes.length + " 字节");
        }
        return bytes;
    }
}
