// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 登录 POST /api/auth/login */
export async function login(
  body: API.LoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResultLoginVO>("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 登出 <p>
JWT 无状态，服务端没有可吊销的东西，前端负责清除本地 token。
保留该接口是为了以后接入黑名单时前端无需改动。 POST /api/auth/logout */
export async function logout(options?: { [key: string]: any }) {
  return request<API.ResultVoid>("/api/auth/logout", {
    method: "POST",
    ...(options || {}),
  });
}

/** 当前登录用户 GET /api/auth/me */
export async function me(options?: { [key: string]: any }) {
  return request<API.ResultUserVO>("/api/auth/me", {
    method: "GET",
    ...(options || {}),
  });
}

/** 修改密码 PUT /api/auth/password */
export async function changePassword(
  body: API.ChangePasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResultVoid>("/api/auth/password", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 注册并自动登录 POST /api/auth/register */
export async function register(
  body: API.RegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResultLoginVO>("/api/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
