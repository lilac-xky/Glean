package com.lilac.auth;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 当前登录用户，不可变
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private final Long userId;

    /**
     * 用户名（冗余自 token，方便记日志）
     */
    private final String username;
}
