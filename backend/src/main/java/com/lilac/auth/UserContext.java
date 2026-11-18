package com.lilac.auth;

import com.lilac.enums.HttpsCodeEnum;
import com.lilac.exception.BusinessException;

/**
 * 当前请求的登录用户上下文
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /**
     * 可能为 null（白名单接口）
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 需要登录才能取，取不到直接 400001
     */
    public static LoginUser getRequired() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(HttpsCodeEnum.NEED_LOGIN);
        }
        return user;
    }

    /**
     * 便捷方法：只要 userId
     */
    public static Long getUserId() {
        return getRequired().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
