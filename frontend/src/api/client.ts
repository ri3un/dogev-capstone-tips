import axios, { AxiosError } from 'axios'
import type { ApiError, ApiResponse } from '../types/api'

// 모든 요청에 JWT를 붙이고, 응답에서는 ApiResponse의 data만 꺼내 줍니다.
export const client = axios.create({ baseURL: '/api' })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (res) => res.data.data,
  (err: AxiosError<ApiResponse<unknown>>) => {
    if (err.response?.status === 401 && location.pathname !== '/login') {
      location.href = '/login'
    }
    const error: ApiError = err.response?.data?.error ?? { code: 'NETWORK', message: '네트워크 오류' }
    return Promise.reject(error)
  },
)

// 인터셉터가 data만 돌려주므로 타입도 그에 맞춘 헬퍼를 씁니다.
export const api = {
  get: <T>(url: string, params?: object) => client.get<T, T>(url, { params }),
  post: <T>(url: string, body?: unknown) => client.post<T, T>(url, body),
  put: <T>(url: string, body?: unknown) => client.put<T, T>(url, body),
  delete: <T>(url: string) => client.delete<T, T>(url),
}
