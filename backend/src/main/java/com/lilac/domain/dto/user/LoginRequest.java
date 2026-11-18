package com.lilac.domain.dto.user;

import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    /**
     * 用户名或邮箱
     */
    private String account;

    /**
     * 密码
     */
    private String password;
}
