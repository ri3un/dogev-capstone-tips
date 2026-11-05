import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import Icon from '../components/common/Icon'
import PlaceDrawer from '../components/place/PlaceDrawer'
import { DOG } from '../mocks/dog'
import { CATEGORY_LABEL, MAP_PLACEHOLDER_URL, PLACES, SEARCH_SUGGESTIONS, STATUS_BG, STATUS_LABEL, formatDistance, type PlaceCategory } from '../mocks/places'

const CATEGORIES = Object.keys(CATEGORY_LABEL) as PlaceCategory[]
const mapBtn = 'flex h-10 w-10 items-center justify-center transition-colors hover:bg-surface-container-low'

// 장소 지도 + 상세 드로어 (Stitch 02-map-drawer)
export default function MapPage() {
  const [params, setParams] = useSearchParams()
  const selectedId = Number(params.get('placeId')) || null // 홈 카드에서 ?placeId=로 열 수 있게
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState<PlaceCategory | null>(null)
  const [dogBased, setDogBased] = useState(true)

  const select = (id: number | null) => setParams(id ? { placeId: String(id) } : {})
  const selected = PLACES.find((p) => p.id === selectedId)

  // TODO: 서버 필터(GET /api/places?category=&dogId=)로 바꿉니다.
  const list = useMemo(
    () =>
      PLACES.filter((p) => (!category || p.category === category) && (!dogBased || p.status !== 'HARD') && p.name.includes(keyword.trim())),
    [category, dogBased, keyword],
  )

  return (
    <div className="flex h-[calc(100vh-80px)] w-full flex-col overflow-hidden lg:flex-row">
      {/* 왼쪽: 검색·필터·목록 */}
      <aside className="z-20 flex h-full w-full shrink-0 flex-col border-r border-outline-variant/30 lg:w-[380px] xl:w-[400px]">
        <div className="flex shrink-0 flex-col gap-3 border-b border-surface-container-high/60 p-4">
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2 rounded-xl border border-beige bg-white px-3.5 py-2.5 shadow-xs transition-all focus-within:ring-2 focus-within:ring-primary-container/40">
              <Icon name="search" className="shrink-0 text-[20px] text-primary-container" />
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="장소 이름이나 동네를 검색해보세요 (예: 평촌 카페)"
                className="w-full min-w-0 bg-transparent text-[13px] placeholder:text-outline focus:outline-none"
              />
              {keyword && (
                <button type="button" title="지우기" onClick={() => setKeyword('')} className="flex items-center justify-center rounded-full p-0.5 text-outline transition-colors hover:bg-surface-container-low hover:text-on-surface">
                  <Icon name="cancel" className="text-[16px]" />
                </button>
              )}
            </div>
            {/* 검색어가 있을 때만 자동완성 표시 (TODO: 검색 API) */}
            {keyword && (
              <ul className="flex flex-col divide-y divide-surface-container-high/40 overflow-hidden rounded-xl border border-beige bg-white shadow-sm">
                {SEARCH_SUGGESTIONS.map((s, i) => (
                  <li key={s.name} onClick={() => setKeyword(s.name)} className="flex cursor-pointer items-center justify-between px-3 py-2.5 transition-colors hover:bg-surface-container-low">
                    <div className="flex min-w-0 items-center gap-2">
                      <Icon name="location_on" className="shrink-0 text-[18px] text-primary-container" />
                      <span className="truncate text-[13px] font-medium">{s.name}</span>
                    </div>
                    <span className="ml-2 shrink-0 text-[12px] text-outline">
                      {s.category} · <span className={i === 0 ? 'font-medium text-primary' : ''}>{formatDistance(s.distanceM)}</span>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="flex items-center justify-between rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-3 shadow-xs">
            <div className="flex flex-col">
              <span className="flex items-center gap-1.5 text-label-lg font-bold">
                <Icon name="pets" className="text-[18px] text-primary" />
                우리 강아지 기준
              </span>
              <span className="mt-0.5 text-[12px] text-outline">
                {DOG.name} · {DOG.size} 기준으로 보여줘요
              </span>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={dogBased}
              onClick={() => setDogBased((v) => !v)}
              className={`flex h-6 w-12 items-center rounded-full p-0.5 shadow-inner transition-colors ${dogBased ? 'justify-end bg-primary-container' : 'justify-start bg-beige'}`}
            >
              <div className="h-5 w-5 rounded-full bg-white shadow-sm" />
            </button>
          </div>

          <div className="flex items-center gap-1.5">
            <div className="flex flex-1 items-center gap-1.5">
              {CATEGORIES.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setCategory(category === c ? null : c)}
                  className={`rounded-full px-3.5 py-1.5 text-[13px] whitespace-nowrap transition-colors ${
                    category === c ? 'bg-primary-container font-bold text-white shadow-xs' : 'border border-outline-variant/30 bg-surface-container-lowest font-medium hover:bg-surface-container-high'
                  }`}
                >
                  {CATEGORY_LABEL[c]}
                </button>
              ))}
            </div>
            {/* TODO: 조건 10개 필터 드로어 */}
            <button type="button" className="flex shrink-0 items-center gap-1 rounded-full border border-outline-variant/30 px-2.5 py-1.5 text-[12px] font-medium whitespace-nowrap text-outline transition-colors hover:bg-surface-container-low hover:text-on-surface">
              <Icon name="tune" className="text-[15px]" />
              필터 더보기
            </button>
          </div>
        </div>

        <div className="flex flex-1 flex-col gap-2.5 overflow-y-auto p-3">
          {list.map((p) => {
            const active = p.id === selectedId
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => select(p.id)}
                className={`flex items-start justify-between rounded-2xl p-3.5 text-left transition-all ${
                  active ? 'border-2 border-primary-container bg-cream shadow-sm hover:shadow-md' : 'border border-outline-variant/30 bg-surface-container-lowest shadow-xs hover:border-outline-variant/60 hover:bg-surface-container-low'
                }`}
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-1.5">
                    <h3 className="truncate text-[16px] font-bold">{p.name}</h3>
                    {active && <span className="h-2 w-2 rounded-full bg-primary-container" />}
                  </div>
                  <div className="mt-1 flex items-center gap-2 text-[13px] text-on-surface-variant">
                    {CATEGORY_LABEL[p.category]}
                    <span className="text-outline">·</span>
                    <span className={active ? 'font-medium text-primary' : 'text-outline'}>{formatDistance(p.distanceM)}</span>
                  </div>
                </div>
                <span className={`shrink-0 rounded-full px-2.5 py-1 text-[12px] font-bold text-white ${STATUS_BG[p.status]}`}>{STATUS_LABEL[p.status]}</span>
              </button>
            )
          })}
          {list.length === 0 && <p className="py-10 text-center text-body-sm text-outline">조건에 맞는 장소가 없어요.</p>}
        </div>
      </aside>

      {/* 오른쪽: 지도 (TODO: 네이버 지도 SDK로 교체, 지금은 배경 이미지 + % 위치 핀) */}
      <div className="relative h-full flex-1 overflow-hidden bg-cream">
        <div className="relative h-full w-full bg-cover bg-center" style={{ backgroundImage: `url('${MAP_PLACEHOLDER_URL}')` }}>
          <div className="pointer-events-none absolute inset-0 bg-cream/20" />
          <div className="absolute top-5 right-5 z-20 flex flex-col gap-2.5">
            <button type="button" title="현재 위치" className={`${mapBtn} rounded-full bg-surface-container-lowest shadow-md`}>
              <Icon name="my_location" className="text-[20px] text-primary" />
            </button>
            <div className="flex flex-col overflow-hidden rounded-full bg-surface-container-lowest shadow-md">
              <button type="button" title="확대" className={mapBtn}>
                <Icon name="add" className="text-[20px]" />
              </button>
              <div className="mx-auto h-px w-6 bg-surface-container" />
              <button type="button" title="축소" className={mapBtn}>
                <Icon name="remove" className="text-[20px]" />
              </button>
            </div>
          </div>

          {list.map((p) => {
            if (!p.pin) return null
            const active = p.id === selectedId
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => select(p.id)}
                style={{ top: p.pin.top, left: p.pin.left }}
                className={`absolute flex -translate-x-1/2 -translate-y-full flex-col items-center ${active ? 'z-20' : 'z-10 opacity-90 transition-opacity hover:opacity-100'}`}
              >
                <span
                  className={`flex items-center gap-1 rounded-full font-bold ${
                    active ? 'bg-primary-container px-3 py-1.5 text-[13px] text-white shadow-lg ring-2 ring-white' : 'border border-outline-variant/30 bg-white px-2.5 py-1 text-[12px] text-secondary shadow-md'
                  }`}
                >
                  <Icon name={p.pin.icon} className="text-[14px]" />
                  {p.name}
                </span>
                <span className={`-mt-1 rotate-45 ${active ? 'h-2.5 w-2.5 bg-primary-container shadow-md' : 'h-2 w-2 border-r border-b border-outline-variant/30 bg-white'}`} />
              </button>
            )
          })}
        </div>

        {selected && <PlaceDrawer place={selected} onClose={() => select(null)} />}
      </div>
    </div>
  )
}
