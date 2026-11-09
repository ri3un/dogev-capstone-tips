// 화면 표시용 작은 변환 함수 모음

/** 오늘 날짜 YYYY-MM-DD (브라우저 현지 시간 기준. toISOString은 UTC라 한국 오전 9시 전에 어제가 됨) */
export const todayStr = () => localDate(new Date())

export const localDate = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

export const addDays = (date: string, days: number) => {
  const d = new Date(`${date}T00:00:00`)
  d.setDate(d.getDate() + days)
  return localDate(d)
}

/** 1km 미만은 m, 이상은 소수 첫째 자리 km */
export const formatDistance = (m: number) => (m < 1000 ? `${m}m` : `${(m / 1000).toFixed(1)}km`)

/**
 * 지도 SDK 붙이기 전 임시 핀 위치: 위·경도 범위를 화면 10~90%(위쪽이 북쪽)에 펼친 % 값
 * shortcut: 실제 지도와 축척이 안 맞음, 네이버 지도 SDK를 붙이면 지울 것
 */
export function spreadPins(points: { lat: number; lng: number }[]) {
  const lats = points.map((p) => p.lat)
  const lngs = points.map((p) => p.lng)
  const [minLat, maxLat, minLng, maxLng] = [Math.min(...lats), Math.max(...lats), Math.min(...lngs), Math.max(...lngs)]
  return points.map((p) => ({
    top: maxLat === minLat ? 50 : 10 + (80 * (maxLat - p.lat)) / (maxLat - minLat),
    left: maxLng === minLng ? 50 : 10 + (80 * (p.lng - minLng)) / (maxLng - minLng),
  }))
}

export const SIZE_LABEL = { SMALL: '소형견', MEDIUM: '중형견', LARGE: '대형견' } as const

export const ageText = (months: number) => (months < 12 ? `${months}개월` : `${Math.floor(months / 12)}살`)

/** "2026-11-08T14:40:00" → "11월 8일" */
export const monthDay = (iso: string) => {
  const d = new Date(iso)
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}
