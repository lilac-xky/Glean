package com.lilac.controller;

import com.lilac.domain.dto.user.ChangePasswordRequest;
import com.lilac.domain.dto.user.LoginRequest;
import com.lilac.domain.dto.user.RegisterRequest;
import com.lilac.domain.result.Result;
import com.lilac.domain.vo.LoginVO;
import com.lilac.domain.vo.UserVO;
import com.lilac.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private SysUserService sysUserService;

    /**
     * 注册并自动登录
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody RegisterRequest request) {
        return Result.success(sysUserService.register(request));
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        return Result.success(sysUserService.login(request));
    }

    /**
     * 当前登录用户
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(sysUserService.getCurrentUser());
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        sysUserService.changePassword(request);
        return Result.success();
    }

    /**
     * 登出
     * <p>
     * JWT 无状态，服务端没有可吊销的东西，前端负责清除本地 token。
     * 保留该接口是为了以后接入黑名单时前端无需改动。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
