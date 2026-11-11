import { Link, Navigate } from 'react-router'
import PawLoading from '../components/common/PawLoading'
import { dogApi } from '../api/endpoints'
import Icon from '../components/common/Icon'
import { card } from '../components/common/styles'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'
import { NUTRITION_SOURCES } from '../constants/care'

const LEVEL = {
  danger: { label: '위험', cls: 'bg-error text-white' },
  warn: { label: '주의', cls: 'bg-status-check text-white' },
  allergy: { label: '알레르기', cls: 'bg-error-container text-on-error-container ring-1 ring-error' },
} as const

// 맞춤 영양 가이드: 체중으로 계산한 하루 열량·사료량·물 + 음식 목록 + 지병 주의점
export default function NutritionPage() {
  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const { data: n, loading: nLoading } = useApi(() => (dogId ? dogApi.nutrition(dogId) : null), [dogId])

  if (loading) return <PawLoading />
  if (!dog) return <Navigate to="/onboarding" replace />
  if (!n) return nLoading ? <PawLoading /> : null

  const { weightKg, factor, foodKcalPerKg, foodGrams, derKcal } = n
  const isAllergen = (name: string) => dog.allergies.some((a) => a.trim() && name.includes(a.trim()))
  // 서버: 위험 → danger, 주의(알레르기 포함) → warn / allergy
  const avoid = [
    ...n.dangerFoods.map((f) => ({ ...f, level: 'danger' as const })),
    ...n.cautionFoods.map((f) => ({ ...f, level: isAllergen(f.name) ? ('allergy' as const) : ('warn' as const) })),
  ]

  return (
    <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-5 py-9 lg:px-10">
      <div>
        <p className="mb-1 text-label-md text-secondary">
          {dog.name} · {dog.breed} · {weightKg}kg 기준
        </p>
        <h1 className="text-headline-lg font-bold tracking-tight">맞춤 영양 가이드</h1>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <section className={`${card} flex flex-col gap-2 p-6`}>
          <h2 className="flex items-center gap-2 text-body-lg font-bold">
            <Icon name="restaurant" className="text-[22px] text-primary" />
            하루 사료량
          </h2>
          {foodGrams ? (
            <p className="text-headline-lg font-bold text-primary">
              {foodGrams}g
            </p>
          ) : (
            <Link to={`/dogs/${dog.id}/edit`} className="text-label-lg font-bold text-primary hover:underline">
              사료 칼로리를 입력하면 g으로 알려드려요 →
            </Link>
          )}
          <p className="text-label-md text-on-surface-variant">
            하루 {derKcal}kcal{foodKcalPerKg && ` · 사료 ${foodKcalPerKg.toLocaleString()}kcal/kg`}
          </p>
          <p className="text-label-sm text-outline">기초 열량(70 × 체중^0.75) × {n.lifeStage} 계수 {factor}</p>
        </section>
        <section className={`${card} flex flex-col gap-2 p-6`}>
          <h2 className="flex items-center gap-2 text-body-lg font-bold">
            <Icon name="water_drop" className="text-[22px] text-primary" />
            하루 물 양
          </h2>
          <p className="text-headline-lg font-bold text-primary">
            {n.waterMinMl}~{n.waterMaxMl}ml
          </p>
          <p className="text-label-md text-on-surface-variant">체중 1kg당 {Math.round(n.waterMinMl / weightKg)}~{Math.round(n.waterMaxMl / weightKg)}ml</p>
          <p className="text-label-sm text-outline">습식 사료·날씨·활동량에 따라 달라져요.</p>
        </section>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <section className={`${card} p-6`}>
          <h2 className="mb-3 flex items-center gap-2 text-body-lg font-bold">
            <Icon name="block" className="text-[22px] text-error" />
            먹으면 안 되는 음식
          </h2>
          <ul className="flex flex-col divide-y divide-outline-variant/30">
            {avoid.map((f) => (
              <li key={f.name} className="flex items-center gap-3 py-2.5">
                <span className={`w-16 shrink-0 rounded-md py-0.5 text-center text-label-sm font-bold ${LEVEL[f.level].cls}`}>{LEVEL[f.level].label}</span>
                <span className="font-semibold">{f.name}</span>
                <span className="ml-auto text-right text-label-md text-outline">{f.reason}</span>
              </li>
            ))}
          </ul>
        </section>
        <section className={`${card} p-6`}>
          <h2 className="mb-3 flex items-center gap-2 text-body-lg font-bold">
            <Icon name="check_circle" className="text-[22px] text-status-ok" />
            먹어도 되는 음식
          </h2>
          <ul className="grid grid-cols-2 gap-2">
            {n.safeFoods.map((f) => (
              <li key={f.name} className="rounded-xl bg-surface-container-low px-3 py-2">
                <p className="text-label-lg font-semibold">{f.name}</p>
                <p className="text-label-sm text-outline">{f.howToFeed}</p>
              </li>
            ))}
          </ul>
          <p className="mt-3 text-label-sm text-secondary">간식은 하루 열량의 10% 이내로 주세요. (약 {Math.round(derKcal * 0.1)}kcal)</p>
        </section>
      </div>

      {n.diseaseCautions.map((d) => (
        <section key={d.diseaseKey} className={`${card} flex flex-col gap-2 bg-gradient-to-br from-surface-container-lowest to-cream p-6`}>
          <h2 className="flex items-center gap-2 text-body-lg font-bold">
            <Icon name="healing" className="text-[22px] text-primary" />
            {d.label} 주의점
          </h2>
          <p className="text-on-surface-variant">· {d.cautionText}</p>
        </section>
      ))}

      <footer className="flex flex-col items-center gap-2 text-label-sm text-outline">
        <div className="flex flex-wrap justify-center gap-3">
          {NUTRITION_SOURCES.map((s) => (
            <a key={s.url} href={s.url} target="_blank" rel="noreferrer" className="underline hover:text-primary">
              {s.label}
            </a>
          ))}
        </div>
        {n.notices.map((t) => (
          <p key={t}>{t}</p>
        ))}
      </footer>
    </div>
  )
}
