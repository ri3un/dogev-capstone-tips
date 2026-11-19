import { useSyncExternalStore } from 'react'

export type LiveLocation = { lat: number; lng: number; accuracy: number }

// 지도가 여러 개 떠도 watchPosition 은 앱 전체에서 하나만 돌림
let current: LiveLocation | null = null
let watchId: number | null = null
const listeners = new Set<() => void>()

function subscribe(listener: () => void) {
  listeners.add(listener)
  if (watchId === null && 'geolocation' in navigator) {
    watchId = navigator.geolocation.watchPosition(
      (p) => {
        current = { lat: p.coords.latitude, lng: p.coords.longitude, accuracy: p.coords.accuracy }
        listeners.forEach((l) => l())
      },
      () => {}, // 권한 거부·실패는 조용히 무시 (점만 안 보임)
      { enableHighAccuracy: true, maximumAge: 30_000 },
    )
  }
  return () => {
    listeners.delete(listener)
    // 마지막 구독자가 빠지면 위치 추적도 멈춤
    if (listeners.size === 0 && watchId !== null) {
      navigator.geolocation.clearWatch(watchId)
      watchId = null
    }
  }
}

/** 실시간 내 위치(정확도 m 포함). 못 받으면 계속 null — 대체 좌표를 쓰지 않음 */
export function useLiveLocation() {
  return useSyncExternalStore(subscribe, () => current)
}
