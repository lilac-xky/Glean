package com.lilac.domain.dto.user;

import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest {

    private String oldPassword;

    /**
     * 新密码：8-32 位，必须同时包含字母和数字
     */
    private String newPassword;
}
