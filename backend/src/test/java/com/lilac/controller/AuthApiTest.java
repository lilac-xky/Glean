package com.lilac.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 注册与认证端到端测试
 * <p>
 * 走真实 HTTP 管道（MockMvc），覆盖拦截器、参数校验、全局异常。
 * 本项目不提供用户管理接口，因此只测认证相关行为。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * MockMvc 路径不含 context-path(/api)，与拦截器匹配规则一致
     */
    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private MvcResult registerRaw(String username, String password, String email) throws Exception {
        // 注意：不能用 "email":"%s" 拼 null，那样会得到字符串 "null"，
        // 触发 @Email 校验失败。email 为空时必须省略该字段或输出真正的 JSON null。
        String emailJson = (email == null || email.isBlank())
                ? "null"
                : "\"" + email + "\"";
        String payload = """
                {"username":"%s","password":"%s","email":%s}
                """.formatted(username, password, emailJson);
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn();
    }

    private String register(String username, String password, String email) throws Exception {
        JsonNode node = json(registerRaw(username, password, email));
        assertEquals(200, node.get("code").asInt(), "注册应成功: " + node);
        return node.get("data").get("token").asText();
    }

    // ==================== 注册 ====================

    @Test
    @DisplayName("注册成功：返回 token，id 是字符串，绝不返回 password")
    void registerSuccess() throws Exception {
        String username = "alice01";
        MvcResult result = registerRaw(username, "Passw0rd", "alice01@example.com");
        String raw = result.getResponse().getContentAsString();
        JsonNode node = json(result);

        assertEquals(200, node.get("code").asInt());
        JsonNode data = node.get("data");
        assertNotNull(data.get("token"));
        assertEquals("Bearer", data.get("tokenType").asText());
        // expiresIn 必须是数字，不能因 Jackson 的 Long→String 变成 "7200"
        assertTrue(data.get("expiresIn").isInt(), "expiresIn 应为数字");
        // id 被 JacksonConfig 转成字符串
        assertTrue(data.get("user").get("id").isTextual(), "id 应为字符串");
        // 用户名统一小写
        assertEquals(username, data.get("user").get("username").asText());
        // 关键：响应中绝不能出现 password 字段
        assertFalse(raw.contains("password"), "响应不应包含 password: " + raw);
    }

    @Test
    @DisplayName("注册：昵称缺省时取用户名")
    void registerDefaultNickname() throws Exception {
        JsonNode node = json(registerRaw("bob0001", "Passw0rd", null));
        assertEquals(200, node.get("code").asInt());
        assertEquals("bob0001", node.get("data").get("user").get("nickname").asText());
    }

    @Test
    @DisplayName("注册：用户名大小写归一，Alice 与 alice 视为同一账号")
    void registerCaseInsensitiveUsername() throws Exception {
        assertEquals(200, json(registerRaw("Carol001", "Passw0rd", null)).get("code").asInt());

        // 换大小写重复注册应被拦下
        JsonNode dup = json(registerRaw("carol001", "Passw0rd", null));
        assertEquals(400005, dup.get("code").asInt(), "大小写不同应视为同一账号: " + dup);
    }

    @Test
    @DisplayName("注册：重复用户名 → 400005")
    void registerDuplicateUsername() throws Exception {
        register("dave0001", "Passw0rd", null);
        JsonNode node = json(registerRaw("dave0001", "Passw0rd", null));
        assertEquals(400005, node.get("code").asInt());
        assertTrue(node.get("msg").asText().contains("用户名"), "提示应指向用户名");
    }

    @Test
    @DisplayName("注册：重复邮箱 → 400005")
    void registerDuplicateEmail() throws Exception {
        register("erin0001", "Passw0rd", "shared@example.com");
        JsonNode node = json(registerRaw("erin0002", "Passw0rd", "shared@example.com"));
        assertEquals(400005, node.get("code").asInt());
        assertTrue(node.get("msg").asText().contains("邮箱"), "提示应指向邮箱");
    }

    @Test
    @DisplayName("注册：密码不足 6 位 → 400002")
    void registerWeakPassword() throws Exception {
        JsonNode node = json(registerRaw("frank001", "12345", null));
        assertEquals(400002, node.get("code").asInt());
        assertTrue(node.get("msg").asText().contains("密码"));
    }

    @Test
    @DisplayName("注册：纯字母密码可以注册（不要求字符类型）")
    void registerPasswordWithoutDigit() throws Exception {
        JsonNode node = json(registerRaw("grace001", "abcdefgh", null));
        assertEquals(200, node.get("code").asInt(), "纯字母密码应允许: " + node);
    }

    @Test
    @DisplayName("注册：6 位纯数字密码可以注册")
    void registerSixDigitPassword() throws Exception {
        JsonNode node = json(registerRaw("grace002", "123456", null));
        assertEquals(200, node.get("code").asInt(), "6 位纯数字应允许: " + node);
    }

    @Test
    @DisplayName("注册：用户名太短 → 400002")
    void registerBadUsername() throws Exception {
        JsonNode node = json(registerRaw("ab", "Passw0rd", null));
        assertEquals(400002, node.get("code").asInt());
    }

    @Test
    @DisplayName("注册：邮箱格式错 → 400002")
    void registerBadEmail() throws Exception {
        JsonNode node = json(registerRaw("heidi001", "Passw0rd", "not-an-email"));
        assertEquals(400002, node.get("code").asInt());
    }

    @Test
    @DisplayName("注册：缺字段 → 400002")
    void registerMissingField() throws Exception {
        JsonNode node = json(mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ivan0001"}
                                """))
                .andReturn());
        assertEquals(400002, node.get("code").asInt());
    }

    // ==================== 登录 ====================

    @Test
    @DisplayName("登录：密码错与账号不存在返回完全相同的结果（防账号枚举）")
    void loginWrongPasswordSameAsNotFound() throws Exception {
        register("judy0001", "Passw0rd", null);

        JsonNode wrongPwd = json(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"judy0001","password":"Wrong123"}
                                """))
                .andReturn());
        JsonNode noUser = json(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"nobody999","password":"Wrong123"}
                                """))
                .andReturn());

        assertEquals(400004, wrongPwd.get("code").asInt());
        assertEquals(wrongPwd.get("code"), noUser.get("code"));
        assertEquals(wrongPwd.get("msg"), noUser.get("msg"));
    }

    @Test
    @DisplayName("登录：支持邮箱登录")
    void loginWithEmail() throws Exception {
        register("karl0001", "Passw0rd", "karl0001@example.com");

        JsonNode node = json(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"karl0001@example.com","password":"Passw0rd"}
                                """))
                .andReturn());
        assertEquals(200, node.get("code").asInt(), "邮箱登录应成功: " + node);
    }

    @Test
    @DisplayName("登录：注册后可直接用返回的 token 访问 /me")
    void tokenFromRegisterIsUsable() throws Exception {
        String token = register("laura001", "Passw0rd", null);

        JsonNode node = json(mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn());
        assertEquals(200, node.get("code").asInt());
        assertEquals("laura001", node.get("data").get("username").asText());
        assertFalse(node.get("data").has("password"), "/me 不应返回 password");
    }

    // ==================== 鉴权 ====================

    @Test
    @DisplayName("无 token 访问 /me → 400001")
    void noTokenRejected() throws Exception {
        JsonNode node = json(mockMvc.perform(get("/auth/me")).andReturn());
        assertEquals(400001, node.get("code").asInt());
    }

    @Test
    @DisplayName("伪造 token → 400001")
    void forgedTokenRejected() throws Exception {
        JsonNode node = json(mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer fake.token.value"))
                .andReturn());
        assertEquals(400001, node.get("code").asInt());
    }

    @Test
    @DisplayName("缺少 Bearer 前缀 → 400001")
    void missingBearerPrefix() throws Exception {
        String token = register("mike0001", "Passw0rd", null);
        JsonNode node = json(mockMvc.perform(get("/auth/me")
                        .header("Authorization", token))
                .andReturn());
        assertEquals(400001, node.get("code").asInt());
    }

    @Test
    @DisplayName("注册与登录在白名单，无需 token")
    void authEndpointsAreWhitelisted() throws Exception {
        JsonNode login = json(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"nobody","password":"x"}
                                """))
                .andReturn());
        assertEquals(400004, login.get("code").asInt(), "应走业务逻辑而非被拦截");
    }

    // ==================== 改密 ====================

    @Test
    @DisplayName("改密：原密码错 → 400004；改密成功后旧密码失效、新密码可用")
    void changePasswordFlow() throws Exception {
        String token = register("nancy001", "Passw0rd", null);

        JsonNode wrongOld = json(mockMvc.perform(put("/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"Nope1234","newPassword":"NewPassw0rd"}
                                """))
                .andReturn());
        // 项目未单独定义「原密码错误」错误码，与登录失败共用 400004
        assertEquals(400004, wrongOld.get("code").asInt(), "原密码错误应返回 400004");

        JsonNode ok = json(mockMvc.perform(put("/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"Passw0rd","newPassword":"NewPassw0rd"}
                                """))
                .andReturn());
        assertEquals(200, ok.get("code").asInt(), "改密应成功");

        JsonNode oldLogin = json(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"nancy001","password":"Passw0rd"}
                                """))
                .andReturn());
        assertEquals(400004, oldLogin.get("code").asInt(), "旧密码应失效");

        JsonNode newLogin = json(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"nancy001","password":"NewPassw0rd"}
                                """))
                .andReturn());
        assertEquals(200, newLogin.get("code").asInt(), "新密码应可用");
    }

    @Test
    @DisplayName("用户管理接口已移除，访问返回 404")
    void userManagementRemoved() throws Exception {
        String token = register("oscar001", "Passw0rd", null);
        MvcResult result = mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        assertEquals(404, result.getResponse().getStatus(), "管理端接口应已不存在");
    }
}
