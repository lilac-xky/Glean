package com.lilac.auth;

import com.lilac.config.AuthProperties;
import com.lilac.enums.HttpsCodeEnum;
import com.lilac.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtUtils 单元测试：不启动 Spring 上下文
 * <p>
 * JwtUtils 用 @Resource 字段注入 AuthProperties，
 * 没有构造器可以传参，所以这里用反射塞进去。
 */
class JwtUtilsTest {

    private static final String SECRET = "unit-test-secret-at-least-32-bytes-long!!";

    private JwtUtils jwtUtils;

    /**
     * 造一个配置对象
     */
    private static AuthProperties props(String secret, int expireSeconds) {
        AuthProperties p = new AuthProperties();
        p.setJwtSecret(secret);
        p.setTokenExpireSeconds(expireSeconds);
        p.setIssuer("glean");
        return p;
    }

    /**
     * 反射注入 authProperties 字段
     */
    private static JwtUtils newJwtUtils(AuthProperties properties) {
        try {
            JwtUtils utils = new JwtUtils();
            Field field = JwtUtils.class.getDeclaredField("authProperties");
            field.setAccessible(true);
            field.set(utils, properties);
            return utils;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("注入 AuthProperties 失败", e);
        }
    }

    @BeforeEach
    void setUp() {
        jwtUtils = newJwtUtils(props(SECRET, 7200));
    }

    @Test
    @DisplayName("签发后可解析，userId 与 username 一致")
    void createThenParse() {
        String token = jwtUtils.createToken(1234567890123456789L, "alice");
        assertNotNull(token);

        LoginUser user = jwtUtils.parseToken(token);
        assertEquals(1234567890123456789L, user.getUserId());
        assertEquals("alice", user.getUsername());
    }

    @Test
    @DisplayName("大 ID 不溢出：19 位 Long 原样返回")
    void bigIdDoesNotOverflow() {
        long bigId = Long.MAX_VALUE;
        LoginUser user = jwtUtils.parseToken(jwtUtils.createToken(bigId, "bob"));
        assertEquals(bigId, user.getUserId());
    }

    @Test
    @DisplayName("篡改 payload 后验签失败，返回 400001")
    void tamperedTokenRejected() {
        String token = jwtUtils.createToken(1L, "alice");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 4) + "AAAA." + parts[2];

        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtUtils.parseToken(tampered));
        assertEquals(HttpsCodeEnum.NEED_LOGIN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("空 token 返回 400001")
    void blankTokenRejected() {
        BusinessException ex = assertThrows(BusinessException.class, () -> jwtUtils.parseToken(""));
        assertEquals(HttpsCodeEnum.NEED_LOGIN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("格式不对的字符串返回 400001")
    void malformedTokenRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtUtils.parseToken("this-is-not-a-jwt"));
        assertEquals(HttpsCodeEnum.NEED_LOGIN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("用别的密钥签发的 token 被拒绝")
    void otherKeyRejected() {
        JwtUtils other = newJwtUtils(props("another-secret-also-32-bytes-long-xxxx", 7200));
        String foreign = other.createToken(1L, "eve");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtUtils.parseToken(foreign));
        assertEquals(HttpsCodeEnum.NEED_LOGIN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("已过期的 token 被拒绝")
    void expiredTokenRejected() throws InterruptedException {
        JwtUtils shortLived = newJwtUtils(props(SECRET, 1));
        String token = shortLived.createToken(1L, "alice");
        Thread.sleep(2100);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtUtils.parseToken(token));
        assertEquals(HttpsCodeEnum.NEED_LOGIN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("密钥不足 32 字节时取密钥失败")
    void weakSecretRejected() {
        AuthProperties weak = props("short", 7200);
        assertThrows(IllegalStateException.class, weak::secretBytes);
    }
}
