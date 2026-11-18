-- H2 兼容版建表脚本，仅用于集成测试。
-- 列名与 SysUser 实体一致（camelToUnderline = false，即 camelCase）。
-- 与 docs/db/schema.sql 的差异：
--   * 列名用 camelCase
--   * 去掉 ENGINE / CHARSET / COLLATE（MySQL 专有）
--   * DATETIME 改 TIMESTAMP
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(32)  NOT NULL DEFAULT '',
    email       VARCHAR(128) DEFAULT NULL,
    avatar      VARCHAR(512) DEFAULT NULL,
    lastLoginAt TIMESTAMP    DEFAULT NULL,
    creatTime   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_username ON sys_user (username);
CREATE UNIQUE INDEX uk_email ON sys_user (email);
