import axios from 'axios'
import type { AxiosError, AxiosRequestConfig } from 'axios'

/** 后端统一返回结构 */
export interface Result<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 业务成功码 */
const SUCCESS_CODE = 200

/** 未登录、token 失效 */
const NEED_LOGIN_CODE = 400001

/** token 在 localStorage 中的键 */
const TOKEN_KEY = 'glean_token'

/** 读取本地 token */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** 写入本地 token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 清除本地 token */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

// 接口路径本身已带 /api，开发环境由 vite 代理转发，生产环境同源，因此不设 baseURL
const http = axios.create({ timeout: 10_000 })

// 请求拦截：有 token 就带上，未登录由后端返回 400001
http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：业务码非 200 直接 reject，登录态失效时清 token 回登录页
http.interceptors.response.use(
  (response) => {
    const body = response.data as Result
    if (body.code !== SUCCESS_CODE) {
      if (body.code === NEED_LOGIN_CODE) {
        clearToken()
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.msg))
    }
    return response
  },
  (error: AxiosError<Result>) => {
    // 业务异常走 HTTP 400，真实错误码和提示在响应体里
    const msg = error.response?.data?.msg
    if (msg) {
      return Promise.reject(new Error(msg))
    }
    const timeout = error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
    return Promise.reject(new Error(timeout ? '请求超时，请稍后重试' : '网络异常，请检查网络后重试'))
  },
)

/**
 * 向后端发送请求
 *
 * @param url    接口路径，形如 /api/auth/login
 * @param config axios 请求配置，method、data、params 等
 * @returns 后端统一返回结构，业务数据在 data 中
 */
export default async function request<T = unknown>(
  url: string,
  config: AxiosRequestConfig = {},
): Promise<T> {
  const response = await http.request<T>({ ...config, url })
  return response.data
}
