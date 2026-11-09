import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { FALLBACK_LOCATION } from '../../hooks/useMyLocation'

// 네이버 지도 JS SDK v3 중 여기서 쓰는 것만 적은 타입 (공식 타입 패키지를 따로 넣지 않으려고)
type LatLng = object
export interface NaverMapInstance {
  getZoom(): number
  setZoom(zoom: number, effect?: boolean): void
  setCenter(p: LatLng): void
  panTo(p: LatLng): void
  fitBounds(bounds: object, margin?: object): void
  destroy(): void
}
interface Overlay {
  setMap(map: NaverMapInstance | null): void
}
interface NaverMapsApi {
  Map: new (el: HTMLElement, options: object) => NaverMapInstance
  LatLng: new (lat: number, lng: number) => LatLng
  LatLngBounds: new (sw: LatLng, ne: LatLng) => { extend(p: LatLng): void }
  Point: new (x: number, y: number) => object
  Marker: new (options: object) => Overlay
  Polyline: new (options: object) => Overlay
}
declare global {
  interface Window {
    naver?: { maps: NaverMapsApi }
    navermap_authFailure?: () => void
  }
}

const AUTH_FAILURE = 'navermap-auth-failure'
let loading: Promise<NaverMapsApi> | null = null

// SDK 스크립트는 앱 전체에서 한 번만 붙임. 키는 루트 .env 의 VITE_NCP_MAP_KEY_ID
function loadNaverMaps() {
  if (window.naver?.maps) return Promise.resolve(window.naver.maps)
  loading ??= new Promise<NaverMapsApi>((resolve, reject) => {
    const key = import.meta.env.VITE_NCP_MAP_KEY_ID
    if (!key) return reject(new Error('.env 에 VITE_NCP_MAP_KEY_ID 가 없어요'))
    // 키나 서비스 URL 인증이 실패하면 SDK가 이 전역 함수를 부름
    window.navermap_authFailure = () => window.dispatchEvent(new Event(AUTH_FAILURE))
    const s = document.createElement('script')
    s.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(key)}`
    s.onload = () => (window.naver?.maps ? resolve(window.naver.maps) : reject(new Error('네이버 지도 SDK가 비어 있어요')))
    s.onerror = () => {
      loading = null // 다음 화면에서 다시 시도
      reject(new Error('네이버 지도 SDK를 불러오지 못했어요'))
    }
    document.head.appendChild(s)
  })
  return loading
}

export interface MapPin {
  id: string | number
  lat: number
  lng: number
  zIndex?: number
  node: ReactNode // 핀 모양 (화면 쪽 JSX 그대로). 핀의 기준점은 좌표 한 점이라 node 안에서 translate 로 맞춤
}
type Point = { lat: number; lng: number }

/**
 * 네이버 지도 + React 핀. pins 는 지도 마커 안에 portal 로 그려서 화면의 핀 디자인을 그대로 씀.
 * fitKey 가 바뀔 때마다 pins·path 가 다 보이게 지도를 맞춤.
 */
export default function NaverMap({
  pins,
  path,
  fitKey,
  className,
  onReady,
}: {
  pins: MapPin[]
  path?: Point[]
  fitKey?: string
  className?: string
  onReady?: (map: NaverMapInstance) => void
}) {
  const boxRef = useRef<HTMLDivElement>(null)
  const [map, setMap] = useState<NaverMapInstance | null>(null)
  const [error, setError] = useState('')

  // 지도는 한 번만 만듦
  useEffect(() => {
    let alive = true
    let created: NaverMapInstance | null = null
    const onAuthFailure = () => setError('네이버 지도 인증에 실패했어요 (키·서비스 URL 확인)')
    window.addEventListener(AUTH_FAILURE, onAuthFailure)
    loadNaverMaps().then(
      (maps) => {
        if (!alive || !boxRef.current) return
        created = new maps.Map(boxRef.current, {
          center: new maps.LatLng(FALLBACK_LOCATION.lat, FALLBACK_LOCATION.lng),
          zoom: 14,
        })
        setMap(created)
        onReady?.(created)
      },
      (e: Error) => alive && setError(e.message),
    )
    return () => {
      alive = false
      window.removeEventListener(AUTH_FAILURE, onAuthFailure)
      created?.destroy()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 핀마다 마커 안에 넣을 빈 div. 위치·순서가 바뀔 때만 새로 만듦
  const pinKey = pins.map((p) => `${p.id}:${p.lat}:${p.lng}:${p.zIndex ?? 0}`).join('|')
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const slots = useMemo(
    () =>
      new Map(
        pins.map((p) => {
          const el = document.createElement('div')
          el.style.whiteSpace = 'nowrap' // 마커 칸은 폭이 0이라 이름이 글자마다 줄바꿈되지 않게
          return [p.id, el]
        }),
      ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [pinKey],
  )

  useEffect(() => {
    const maps = window.naver?.maps
    if (!map || !maps) return
    const markers = pins.map(
      (p) =>
        new maps.Marker({
          map,
          position: new maps.LatLng(p.lat, p.lng),
          zIndex: p.zIndex ?? 0,
          icon: { content: slots.get(p.id), anchor: new maps.Point(0, 0) },
        }),
    )
    return () => markers.forEach((m) => m.setMap(null))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, slots])

  // 경로: 흰 테두리 + 점선 (기존 SVG 경로와 같은 모양)
  const pathKey = (path ?? []).map((p) => `${p.lat}:${p.lng}`).join('|')
  useEffect(() => {
    const maps = window.naver?.maps
    if (!map || !maps || !path || path.length < 2) return
    const pts = path.map((p) => new maps.LatLng(p.lat, p.lng))
    const color = getComputedStyle(document.documentElement).getPropertyValue('--color-primary-container').trim()
    const lines = [
      new maps.Polyline({ map, path: pts, strokeColor: '#fff8f5', strokeOpacity: 0.9, strokeWeight: 7, strokeLineCap: 'round' }),
      new maps.Polyline({ map, path: pts, strokeColor: color, strokeWeight: 4.5, strokeStyle: 'shortdash', strokeLineCap: 'round' }),
    ]
    return () => lines.forEach((l) => l.setMap(null))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, pathKey])

  // fitKey 가 바뀌면 핀·경로가 다 보이게
  useEffect(() => {
    const maps = window.naver?.maps
    const pts: Point[] = [...pins, ...(path ?? [])]
    if (!map || !maps || pts.length === 0) return
    const first = new maps.LatLng(pts[0].lat, pts[0].lng)
    if (pts.length === 1) return map.setCenter(first)
    const bounds = new maps.LatLngBounds(first, first)
    pts.forEach((p) => bounds.extend(new maps.LatLng(p.lat, p.lng)))
    map.fitBounds(bounds, { top: 70, right: 70, bottom: 70, left: 70 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, fitKey])

  return (
    <div className={className}>
      {/* 이 div 안은 SDK가 그림 (React 자식 없음) */}
      <div ref={boxRef} className="h-full w-full" />
      {error && <p className="absolute inset-0 flex items-center justify-center text-label-md text-outline">{error}</p>}
      {pins.map((p) => {
        const slot = slots.get(p.id)
        return slot ? createPortal(p.node, slot, String(p.id)) : null
      })}
    </div>
  )
}
