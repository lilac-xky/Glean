-- =============================================================
-- Glean 数据库初始化脚本
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci
-- 日期：2026-11-15
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < docs/db/schema.sql
-- 注意：脚本含 DROP TABLE，仅用于空库初始化；已有数据的环境请勿直接执行。
-- =============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `glean`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `glean`;

-- -------------------------------------------------------------
-- 1. 用户表（必建）
--    设计说明见 docs/features/FEAT-登录与鉴权-方案.md
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username`      VARCHAR(32)  NOT NULL COMMENT '登录账号：4-20 位字母/数字/下划线，统一小写存储',
    `password`      VARCHAR(100) NOT NULL COMMENT '密码哈希：BCrypt（60 字符），预留升级空间',
    `nickname`      VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '昵称，注册时默认取 username',
    `email`         VARCHAR(128)          DEFAULT NULL COMMENT '邮箱，可选；统一小写存储',
    `avatar`        VARCHAR(512)          DEFAULT NULL COMMENT '头像 URL',
    `lastLoginAt` DATETIME              DEFAULT NULL COMMENT '最近登录时间',
    `creatTime`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户表';