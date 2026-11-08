import { useEffect, useState } from 'react'

// 위치 권한이 없거나 실패하면 시연 지역(안양 평촌역)으로
export const FALLBACK_LOCATION = { lat: 37.3943, lng: 126.9568 }

/** 브라우저 현재 위치. 받기 전에는 null */
export function useMyLocation() {
  const [pos, setPos] = useState<{ lat: number; lng: number } | null>(() => ('geolocation' in navigator ? null : FALLBACK_LOCATION))
  useEffect(() => {
    if (!('geolocation' in navigator)) return
    navigator.geolocation.getCurrentPosition(
      (p) => setPos({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => setPos(FALLBACK_LOCATION),
      { timeout: 5000, maximumAge: 600_000 },
    )
  }, [])
  return pos
}
