import axios, { AxiosError } from 'axios'
import type { ApiError, ApiResponse } from '../types/api'

// 모든 요청에 JWT를 붙이고, 응답에서는 ApiResponse의 data만 꺼내 줍니다.
export const client = axios.create({ baseURL: '/api' })

export const TOKEN_KEY = 'accessToken'

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (res) => (res.config.responseType === 'blob' ? res.data : res.data.data),
  async (err: AxiosError<ApiResponse<unknown> | Blob>) => {
    if (err.response?.status === 401 && location.pathname !== '/login') {
      localStorage.removeItem(TOKEN_KEY)
      location.href = '/login'
    }
    let body = err.response?.data
    // PDF처럼 blob으로 받은 요청의 오류는 JSON으로 다시 읽음
    if (body instanceof Blob) body = JSON.parse(await body.text()) as ApiResponse<unknown>
    const error: ApiError = body?.error ?? { code: 'NETWORK', message: '서버에 연결하지 못했어요' }
    return Promise.reject(error)
  },
)

// 인터셉터가 data만 돌려주므로 타입도 그에 맞춘 헬퍼를 씁니다.
export const api = {
  get: <T>(url: string, params?: object) => client.get<T, T>(url, { params }),
  post: <T>(url: string, body?: unknown) => client.post<T, T>(url, body),
  put: <T>(url: string, body?: unknown) => client.put<T, T>(url, body),
  patch: <T>(url: string, body?: unknown) => client.patch<T, T>(url, body),
  delete: <T>(url: string) => client.delete<T, T>(url),
  upload: <T>(url: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return client.post<T, T>(url, form)
  },
  blob: (url: string, params?: object) => client.get<Blob, Blob>(url, { params, responseType: 'blob' }),
}
