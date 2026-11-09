import axios from 'axios'

// 从环境变量中读取（在 .env.development 中配置的 /api）
const baseURL = import.meta.env.VITE_API_BASE_URL

const request = axios.create({
  baseURL: baseURL,
  timeout: 5000,
})

// 示例：登录接口
export const login = (data: any) => {
  // 实际请求路径为 /api/user/login
  // Vite 会拦截 /api 并转发到 http://localhost:8080/user/login
  return request.post('/user/login', data)
}