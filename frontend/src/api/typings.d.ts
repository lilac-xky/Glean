declare namespace API {
  type ChangePasswordRequest = {
    oldPassword?: string;
    /** 新密码：8-32 位，必须同时包含字母和数字 */
    newPassword?: string;
  };

  type LoginRequest = {
    /** 用户名或邮箱 */
    account?: string;
    /** 密码 */
    password?: string;
  };

  type LoginVO = {
    /** JWT 令牌 */
    token?: string;
    /** 固定 Bearer */
    tokenType?: string;
    /** 有效期（秒）。
必须用 int：Jackson 配置把所有 Long 序列化成字符串，"7200" 会让前端算错。 */
    expiresIn?: number;
    /** 脱敏后的用户信息 */
    user?: UserVO;
  };

  type RegisterRequest = {
    /** 用户名：4-20 位字母/数字/下划线 */
    username?: string;
    /** 密码：8-32 位，必须同时包含字母和数字 */
    password?: string;
    /** 邮箱，可选 */
    email?: string;
    /** 昵称，可选，缺省取用户名 */
    nickname?: string;
  };

  type ResultLoginVO = {
    code?: number;
    msg?: string;
    data?: LoginVO;
  };

  type ResultUserVO = {
    code?: number;
    msg?: string;
    /** 脱敏后的用户信息 */
    data?: UserVO;
  };

  type ResultVoid = {
    code?: number;
    msg?: string;
    data?: null;
  };

  type UserVO = {
    id?: number;
    username?: string;
    nickname?: string;
    email?: string;
    avatar?: string;
    lastLoginAt?: string;
    creatTime?: string;
  };
}
