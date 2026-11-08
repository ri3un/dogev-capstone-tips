import { useCallback, useEffect, useState } from 'react'
import type { ApiError } from '../types/api'

/**
 * 화면에서 서버 데이터를 불러오는 공통 훅. deps가 바뀌면 다시 부르고, 늦게 온 옛 응답은 버립니다.
 * load가 null을 돌려주면(예: 반려견이 아직 없음) 부르지 않습니다.
 * shortcut: 캐시·재시도가 없음. 같은 데이터를 여러 화면에서 자주 부르게 되면 TanStack Query로 바꿀 것
 */
export function useApi<T>(load: () => Promise<T> | null, deps: unknown[]) {
  const [data, setData] = useState<T>()
  const [error, setError] = useState<ApiError>()
  const [loading, setLoading] = useState(true)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const p = load()
    if (!p) {
      setLoading(false)
      return
    }
    let alive = true
    setLoading(true)
    setError(undefined)
    p.then(
      (d) => alive && setData(d),
      (e: ApiError) => alive && setError(e),
    ).finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])
  return { data, error, loading, reload, setData }
}
