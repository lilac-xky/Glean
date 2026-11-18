package com.lilac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录/注册响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JWT 令牌
     */
    private String token;

    /**
     * 固定 Bearer
     */
    private String tokenType;

    /**
     * 有效期（秒）。
     * 必须用 int：Jackson 配置把所有 Long 序列化成字符串，"7200" 会让前端算错。
     */
    private Integer expiresIn;

    /**
     * 脱敏后的用户信息
     */
    private UserVO user;

    public static LoginVO of(String token, int expiresIn, UserVO user) {
        return new LoginVO(token, "Bearer", expiresIn, user);
    }
}
