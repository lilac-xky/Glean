package com.lilac.domain.dto.user;

import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {

    /**
     * 用户名：4-20 位字母/数字/下划线
     */
    private String username;

    /**
     * 密码：8-32 位，必须同时包含字母和数字
     */
    private String password;

    /**
     * 邮箱，可选
     */
    private String email;

    /**
     * 昵称，可选，缺省取用户名
     */
    private String nickname;
}
