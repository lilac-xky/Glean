package com.lilac.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.lilac.auth.JwtUtils;
import com.lilac.auth.UserContext;
import com.lilac.domain.dto.user.ChangePasswordRequest;
import com.lilac.domain.dto.user.LoginRequest;
import com.lilac.domain.dto.user.RegisterRequest;
import com.lilac.domain.entity.SysUser;
import com.lilac.domain.vo.LoginVO;
import com.lilac.domain.vo.UserVO;
import com.lilac.enums.HttpsCodeEnum;
import com.lilac.exception.BusinessException;
import com.lilac.mapper.SysUserMapper;
import com.lilac.service.SysUserService;
import com.lilac.utils.ThrowUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.regex.Pattern;

import static com.lilac.domain.entity.table.SysUserTableDef.SYS_USER;

/**
 * 用户服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * BCrypt 计算强度，10 是安全与耗时的常用平衡点
     */
    private static final int BCRYPT_COST = 10;

    /**
     * 用户名：4-20 位字母、数字或下划线
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");

    /**
     * 邮箱格式，宽松校验：只要求存在 @ 与点号分段
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 密码只限制长度，不要求字符类型
     */
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 32;
    private static final int EMAIL_MAX = 128;
    private static final int NICKNAME_MAX = 32;

    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterRequest request) {
        ThrowUtils.throwIf(request == null, HttpsCodeEnum.PARAMS_ERROR, "请求体不能为空");

        String username = normalize(request.getUsername());
        validateUsername(username);
        validatePassword(request.getPassword(), "密码");
        String email = normalizeNullable(request.getEmail());
        validateOptionalEmail(email);
        String nickname = normalizeNickname(request.getNickname());

        // 两次独立查询：用 or 拼会在 email 为 null 时生成 "email = null"，永远不匹配
        ThrowUtils.throwIf(findByUsername(username) != null,
                HttpsCodeEnum.USER_EXIST, "用户名已被占用");
        if (email != null) {
            ThrowUtils.throwIf(findByEmail(email) != null,
                    HttpsCodeEnum.USER_EXIST, "邮箱已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt(BCRYPT_COST)));
        user.setNickname(nickname == null ? username : nickname);
        user.setEmail(email);

        try {
            // insertSelective：忽略 null 字段，让 DDL 的默认值生效
            mapper.insertSelective(user);
        } catch (DuplicateKeyException e) {
            // 前置查重挡不住并发，唯一索引是最后一道防线
            log.warn("注册并发冲突: username={}, email={}", username, email);
            throw new BusinessException(HttpsCodeEnum.USER_EXIST);
        }

        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        ThrowUtils.throwIf(request == null, HttpsCodeEnum.PARAMS_ERROR, "请求体不能为空");

        String account = normalize(request.getAccount());
        ThrowUtils.throwIf(StrUtil.isBlank(account), HttpsCodeEnum.PARAMS_ERROR, "账号不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getPassword()),
                HttpsCodeEnum.PARAMS_ERROR, "密码不能为空");

        // 先按用户名查，查不到再按邮箱
        SysUser user = findByUsername(account);
        if (user == null) {
            user = findByEmail(account);
        }
        // 不区分「账号不存在」与「密码错误」，避免账号枚举
        ThrowUtils.throwIf(user == null, HttpsCodeEnum.USER_OR_PASSWORD_ERROR);

        // 先验密码。不区分「账号不存在」与「密码错误」，避免账号枚举
        ThrowUtils.throwIf(!BCrypt.checkpw(request.getPassword(), user.getPassword()),
                HttpsCodeEnum.USER_OR_PASSWORD_ERROR);

        // 只更新登录时间。updateById 会整行覆盖，把没赋值的字段清空
        Date now = new Date();
        UpdateChain.of(SysUser.class)
                .set(SysUser::getLastLoginAt, now)
                .where(SysUser::getId).eq(user.getId())
                .update();

        user.setLastLoginAt(now);
        return buildLoginVO(user);
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = UserContext.getUserId();
        SysUser user = getById(userId);
        // token 有效但用户已被删除
        ThrowUtils.throwIf(user == null, HttpsCodeEnum.NEED_LOGIN);
        return UserVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        ThrowUtils.throwIf(request == null, HttpsCodeEnum.PARAMS_ERROR, "请求体不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getOldPassword()),
                HttpsCodeEnum.PARAMS_ERROR, "原密码不能为空");
        validatePassword(request.getNewPassword(), "新密码");

        Long userId = UserContext.getUserId();
        SysUser user = getById(userId);
        ThrowUtils.throwIf(user == null, HttpsCodeEnum.NEED_LOGIN);
        ThrowUtils.throwIf(!BCrypt.checkpw(request.getOldPassword(), user.getPassword()),
                HttpsCodeEnum.USER_OR_PASSWORD_ERROR);

        UpdateChain.of(SysUser.class)
                .set(SysUser::getPassword, BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt(BCRYPT_COST)))
                .where(SysUser::getId).eq(userId)
                .update();
    }

    private static void validateUsername(String username) {
        ThrowUtils.throwIf(StrUtil.isBlank(username),
                HttpsCodeEnum.PARAMS_ERROR, "用户名不能为空");
        ThrowUtils.throwIf(!USERNAME_PATTERN.matcher(username).matches(),
                HttpsCodeEnum.PARAMS_ERROR, "用户名需为 4-20 位字母、数字或下划线");
    }

    /**
     * 密码规则：6-32 位，不限字符类型
     *
     * @param fieldName 用于错误提示，如「密码」「新密码」
     */
    private static void validatePassword(String password, String fieldName) {
        ThrowUtils.throwIf(StrUtil.isBlank(password),
                HttpsCodeEnum.PARAMS_ERROR, fieldName + "不能为空");
        ThrowUtils.throwIf(password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX,
                HttpsCodeEnum.PARAMS_ERROR, fieldName + "长度需为 6-32 位");
    }

    private static void validateOptionalEmail(String email) {
        if (email == null) {
            return;
        }
        ThrowUtils.throwIf(email.length() > EMAIL_MAX,
                HttpsCodeEnum.PARAMS_ERROR, "邮箱长度不能超过 128");
        ThrowUtils.throwIf(!EMAIL_PATTERN.matcher(email).matches(),
                HttpsCodeEnum.PARAMS_ERROR, "邮箱格式不正确");
    }

    /**
     * 昵称可选；为空返回 null，由调用方决定回退值
     */
    private static String normalizeNickname(String nickname) {
        if (StrUtil.isBlank(nickname)) {
            return null;
        }
        String trimmed = nickname.trim();
        ThrowUtils.throwIf(trimmed.length() > NICKNAME_MAX,
                HttpsCodeEnum.PARAMS_ERROR, "昵称长度不能超过 32");
        return trimmed;
    }

    private LoginVO buildLoginVO(SysUser user) {
        String token = jwtUtils.createToken(user.getId(), user.getUsername());
        return LoginVO.of(token, jwtUtils.getExpireSeconds(), UserVO.from(user));
    }

    private SysUser findByUsername(String username) {
        return mapper.selectOneByQuery(
                QueryWrapper.create().and(SYS_USER.USERNAME.eq(username)));
    }

    private SysUser findByEmail(String email) {
        return mapper.selectOneByQuery(
                QueryWrapper.create().and(SYS_USER.EMAIL.eq(email)));
    }

    /**
     * 统一转小写去空格。uk_username 排序规则不区分大小写，
     * 不统一的话 Alice 与 alice 会注册成两个账号或撞唯一键
     */
    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private static String normalizeNullable(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
