// 백엔드 공통 응답 형식 (결정 18). 백엔드 ApiResponse와 같은 모양입니다.
export interface ApiError {
  code: string
  message: string
}

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: ApiError | null
}
