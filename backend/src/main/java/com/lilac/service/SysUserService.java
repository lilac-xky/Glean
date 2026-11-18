package com.lilac.service;

import com.lilac.domain.dto.user.ChangePasswordRequest;
import com.lilac.domain.dto.user.LoginRequest;
import com.lilac.domain.dto.user.RegisterRequest;
import com.lilac.domain.entity.SysUser;
import com.lilac.domain.vo.LoginVO;
import com.lilac.domain.vo.UserVO;
import com.mybatisflex.core.service.IService;

/**
 * 用户服务
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 注册并自动登录
     */
    LoginVO register(RegisterRequest request);

    /**
     * 登录
     */
    LoginVO login(LoginRequest request);

    /**
     * 当前登录用户信息
     */
    UserVO getCurrentUser();

    /**
     * 修改密码
     */
    void changePassword(ChangePasswordRequest request);
}
