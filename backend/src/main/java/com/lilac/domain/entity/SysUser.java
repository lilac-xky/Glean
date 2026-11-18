package com.lilac.domain.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "sys_user", camelToUnderline = false)
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 登录账号：4-20 位字母/数字/下划线，统一小写存储
     */
    private String username;

    /**
     * 密码哈希：BCrypt（60 字符），预留升级空间
     */
    private String password;

    /**
     * 昵称，注册时默认取 username
     */
    private String nickname;

    /**
     * 邮箱，可选；统一小写存储
     */
    private String email;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 最近登录时间
     */
    private Date lastLoginAt;

    /**
     * 创建时间
     */
    private Date creatTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
