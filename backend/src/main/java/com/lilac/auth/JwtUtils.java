package com.lilac.auth;

import cn.hutool.core.convert.Convert;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.lilac.config.AuthProperties;
import com.lilac.enums.HttpsCodeEnum;
import com.lilac.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具：签发与解析
 */
@Component
@Slf4j
public class JwtUtils {

    /**
     * 配置（密钥、过期时间、签发者）。
     */
    @Resource
    private AuthProperties authProperties;

    /**
     * 便捷方法：每次取密钥都走这里，避免各处重复转换
     */
    private byte[] key() {
        return authProperties.secretBytes();
    }

    public int getExpireSeconds() {
        return authProperties.getTokenExpireSeconds();
    }

    /**
     * 签发 token
     */
    public String createToken(Long userId, String username) {
        if(userId == null){
            throw new IllegalArgumentException("userId 为空");
        }
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiresAt = new Date(now + authProperties.getTokenExpireSeconds() * 1000L);
        return JWT.create()
                .setPayload(JWTPayload.SUBJECT, userId)
                .setPayload("username", username)
                .setPayload(JWTPayload.ISSUER, authProperties.getIssuer())
                .setIssuedAt(issuedAt)
                .setExpiresAt(expiresAt)
                .setSigner(JWTSignerUtil.hs256(key()))
                .setKey(key())
                .sign();
    }

    /**
     * 解析并校验 token
     */
    public LoginUser parseToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException(HttpsCodeEnum.NEED_LOGIN);
        }

        JWT jwt;
        try {
            JWTValidator.of(token)
                    .validateAlgorithm(JWTSignerUtil.hs256(key()))
                    .validateDate();
            jwt = JWT.of(token);
        } catch (Exception e) {
            log.debug("token 校验失败: {}", e.getMessage());
            throw new BusinessException(HttpsCodeEnum.NEED_LOGIN);
        }

        Long userId = Convert.toLong(jwt.getPayload(JWTPayload.SUBJECT), null);
        if (userId == null) {
            throw new BusinessException(HttpsCodeEnum.NEED_LOGIN);
        }

        String issuer = Convert.toStr(jwt.getPayload(JWTPayload.ISSUER));
        if (issuer == null || !issuer.equals(authProperties.getIssuer())) {
            throw new BusinessException(HttpsCodeEnum.NEED_LOGIN);
        }

        String username = Convert.toStr(jwt.getPayload("username"));
        return new LoginUser(userId, username);
    }
}
