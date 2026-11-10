import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router'
import { dogApi, familyApi } from '../api/endpoints'
import { todayStr } from '../api/format'
import type { Dog, DogInput } from '../api/types'
import BreedSearch from '../components/common/BreedSearch'
import ConfirmModal from '../components/common/ConfirmModal'
import Icon from '../components/common/Icon'
import Photo from '../components/common/Photo'
import { card, chip, ghostBtn, input, primaryBtn } from '../components/common/styles'
import { useApi } from '../hooks/useApi'

type Sex = 'MALE' | 'FEMALE'
interface Form {
  name: string
  breed: string
  weight: string
  birth: string // yyyy-MM
  sex: Sex | ''
  neutered: boolean | null
  memo: string
  // 수정 화면의 선택 항목 (영양·코스 추천에 쓰임)
  activity: 'LOW' | 'NORMAL' | 'HIGH'
  diseases: string[]
  allergies: string
  foodKcal: string
  carrier: boolean
}

const thisMonth = todayStr().slice(0, 7)
const DISEASES = ['신장', '췌장염', '비만', '관절', '피부']
// 화면 지병 이름 ↔ 서버 키 (영양 카드·코스 규칙이 키로 읽음)
const DISEASE_KEY: Record<string, string> = { 신장: 'KIDNEY', 췌장염: 'PANCREATITIS', 비만: 'OBESITY', 관절: 'JOINT', 피부: 'SKIN' }
const DISEASE_LABEL = Object.fromEntries(Object.entries(DISEASE_KEY).map(([k, v]) => [v, k]))
const ACTIVITY = { LOW: '낮음', NORMAL: '보통', HIGH: '높음' } as const
const EMPTY: Form = { name: '', breed: '', weight: '', birth: '', sex: '', neutered: null, memo: '', activity: 'NORMAL', diseases: [], allergies: '', foodKcal: '', carrier: false }

const toForm = (d: Dog): Form => ({
  name: d.name, breed: d.breed, weight: String(d.weightKg), birth: d.birthYm, sex: d.sex, neutered: d.neutered,
  memo: d.memo ?? '', activity: d.activityLevel ?? 'NORMAL', diseases: d.diseases.map((x) => DISEASE_LABEL[x] ?? x),
  allergies: d.allergies.join(', '), foodKcal: d.foodKcalPerKg ? String(d.foodKcalPerKg) : '', carrier: d.hasCarrier,
})
const toInput = (f: Form): DogInput => ({
  name: f.name.trim(), breed: f.breed.trim(), weightKg: Number(f.weight), birthYm: f.birth, sex: f.sex || 'MALE',
  neutered: Boolean(f.neutered), memo: f.memo.trim() || null, activityLevel: f.activity, hasCarrier: f.carrier,
  diseases: f.diseases.map((d) => DISEASE_KEY[d] ?? d),
  allergies: f.allergies.split(',').map((a) => a.trim()).filter(Boolean),
  foodKcalPerKg: f.foodKcal ? Number(f.foodKcal) : null,
})

// 몸무게로 크기 자동 분류 (10kg 미만 소형, 25kg 미만 중형)
const sizeOf = (kg: number) => (kg < 10 ? '소형견' : kg < 25 ? '중형견' : '대형견')

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-label-lg font-semibold">
        {label} <span className="text-primary">*</span>
      </span>
      {children}
      {error && <span className="text-label-sm text-error">{error}</span>}
    </div>
  )
}

// 반려견 등록(/dogs/new)·수정(/dogs/:dogId/edit) 공용 폼: 필수 6개 + 건강 메모, 수정 때만 선택 항목·삭제
export default function DogFormPage() {
  const { dogId } = useParams()
  const isEdit = Boolean(dogId)
  const navigate = useNavigate()
  const [form, setForm] = useState<Form>(EMPTY)
  const [submitted, setSubmitted] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [saving, setSaving] = useState(false)
  const [serverError, setServerError] = useState('')
  const [photo, setPhoto] = useState<{ file: File; preview: string }>()
  const fileRef = useRef<HTMLInputElement>(null)
  const set = <K extends keyof Form>(key: K, value: Form[K]) => setForm((f) => ({ ...f, [key]: value }))

  const { data: dog } = useApi(() => (dogId ? dogApi.get(Number(dogId)) : null), [dogId])
  const { data: family } = useApi(() => familyApi.get(), [])
  useEffect(() => {
    if (dog) setForm(toForm(dog))
  }, [dog])

  const kg = Number(form.weight)
  const errors = {
    name: !form.name.trim() ? '이름을 입력해 주세요.' : '',
    breed: !form.breed.trim() ? '견종을 입력해 주세요.' : '',
    weight: !(kg >= 0.5 && kg <= 90) || !/^\d+(\.\d)?$/.test(form.weight) ? '0.5~90kg, 소수 첫째 자리까지 입력해 주세요.' : '',
    birth: !form.birth || form.birth > thisMonth ? '생년월을 골라 주세요. (미래 날짜는 안 돼요)' : '',
    sex: !form.sex ? '성별을 골라 주세요.' : '',
    neutered: form.neutered === null ? '중성화 여부를 골라 주세요.' : '',
  }
  const ok = Object.values(errors).every((e) => !e)
  const err = (k: keyof typeof errors) => (submitted ? errors[k] : '')

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSubmitted(true)
    if (!ok || saving) return
    setSaving(true)
    setServerError('')
    try {
      const saved = dog ? await dogApi.update(dog.id, toInput(form)) : await dogApi.create(toInput(form))
      if (photo) await dogApi.photo(saved.id, photo.file)
      if (!dog) localStorage.setItem('dogId', String(saved.id))
      navigate('/')
    } catch (err) {
      setServerError((err as { message?: string }).message ?? '저장하지 못했어요')
      setSaving(false)
    }
  }

  const pickPhoto = (file?: File) => {
    if (file) setPhoto({ file, preview: URL.createObjectURL(file) })
  }

  const remove = async () => {
    if (!dog) return
    await dogApi.remove(dog.id)
    localStorage.removeItem('dogId')
    navigate('/my')
  }

  return (
    <div className="mx-auto flex w-full max-w-[760px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div>
        <p className="mb-1 text-label-md text-secondary">{isEdit ? '반려견 정보' : '처음 한 번만 입력해요'}</p>
        <h1 className="text-headline-lg font-bold tracking-tight">{isEdit ? `${form.name || '반려견'} 정보 수정` : '반려견 등록'}</h1>
      </div>

      <form onSubmit={submit} noValidate className={`${card} flex flex-col gap-6 p-6 md:p-8`}>
        <div className="flex items-center gap-5">
          <div className="relative">
            <Photo src={photo?.preview ?? dog?.photoUrl ?? undefined} alt="반려견 사진" className="h-24 w-24 rounded-full object-cover ring-4 ring-secondary-container" />
            <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(e) => pickPhoto(e.target.files?.[0])} />
            <button type="button" aria-label="사진 바꾸기" onClick={() => fileRef.current?.click()} className="absolute -right-1 -bottom-1 flex h-9 w-9 items-center justify-center rounded-full bg-primary-container text-on-primary shadow">
              <Icon name="photo_camera" className="text-[18px]" />
            </button>
          </div>
          <p className="text-label-md text-outline">사진은 선택이에요. 나중에 넣어도 돼요.</p>
        </div>

        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          <Field label="이름" error={err('name')}>
            <input value={form.name} onChange={(e) => set('name', e.target.value)} maxLength={20} placeholder="예: 꿍" className={input} />
          </Field>
          <Field label="견종" error={err('breed')}>
            <BreedSearch value={form.breed} onChange={(v) => set('breed', v)} />
          </Field>
          <Field label="몸무게" error={err('weight')}>
            <div className="relative">
              <input value={form.weight} onChange={(e) => set('weight', e.target.value)} inputMode="decimal" placeholder="예: 3.2" className={`${input} pr-12`} />
              <span className="absolute top-1/2 right-4 -translate-y-1/2 text-outline">kg</span>
            </div>
            {!errors.weight && (
              <span className="flex items-center gap-1 text-label-sm text-secondary">
                <Icon name="straighten" className="text-[16px]" />
                자동 분류: <strong>{sizeOf(kg)}</strong>
              </span>
            )}
          </Field>
          <Field label="생년월" error={err('birth')}>
            <input type="month" value={form.birth} max={thisMonth} onChange={(e) => set('birth', e.target.value)} className={input} />
          </Field>
          <Field label="성별" error={err('sex')}>
            <div className="flex gap-2">
              {(['MALE', 'FEMALE'] as Sex[]).map((s) => (
                <button key={s} type="button" onClick={() => set('sex', s)} className={`${chip(form.sex === s)} flex-1`}>
                  {s === 'MALE' ? '남아' : '여아'}
                </button>
              ))}
            </div>
          </Field>
          <Field label="중성화" error={err('neutered')}>
            <div className="flex gap-2">
              {[true, false].map((v) => (
                <button key={String(v)} type="button" onClick={() => set('neutered', v)} className={`${chip(form.neutered === v)} flex-1`}>
                  {v ? '했어요' : '안 했어요'}
                </button>
              ))}
            </div>
          </Field>
        </div>

        <label className="flex flex-col gap-2">
          <span className="text-label-lg font-semibold">
            건강 메모 <span className="text-label-sm font-normal text-outline">(선택)</span>
          </span>
          <textarea value={form.memo} onChange={(e) => set('memo', e.target.value)} rows={3} placeholder="알레르기, 지병, 주의할 점 (예: 슬개골 주의)" className={`${input} h-auto resize-none py-3`} />
        </label>

        {isEdit && (
          <fieldset className="flex flex-col gap-5 rounded-2xl bg-surface-container-low p-5">
            <legend className="sr-only">선택 항목</legend>
            <p className="text-label-md text-secondary">선택 항목: 영양 가이드와 코스 추천이 더 정확해져요</p>
            <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
              <div className="flex flex-col gap-2">
                <span className="text-label-lg font-semibold">활동량</span>
                <div className="flex gap-2">
                  {(Object.keys(ACTIVITY) as Form['activity'][]).map((a) => (
                    <button key={a} type="button" onClick={() => set('activity', a)} className={`${chip(form.activity === a)} flex-1`}>
                      {ACTIVITY[a]}
                    </button>
                  ))}
                </div>
              </div>
              <label className="flex flex-col gap-2">
                <span className="text-label-lg font-semibold">사료 칼로리</span>
                <div className="relative">
                  <input value={form.foodKcal} onChange={(e) => set('foodKcal', e.target.value.replace(/\D/g, ''))} inputMode="numeric" placeholder="사료 봉투 뒷면 (예: 3300)" className={`${input} pr-20`} />
                  <span className="absolute top-1/2 right-4 -translate-y-1/2 text-outline">kcal/kg</span>
                </div>
              </label>
              <div className="flex flex-col gap-2">
                <span className="text-label-lg font-semibold">지병 (여러 개)</span>
                <div className="flex flex-wrap gap-2">
                  {DISEASES.map((d) => {
                    const on = form.diseases.includes(d)
                    return (
                      <button key={d} type="button" onClick={() => set('diseases', on ? form.diseases.filter((x) => x !== d) : [...form.diseases, d])} className={chip(on)}>
                        {d}
                      </button>
                    )
                  })}
                </div>
              </div>
              <label className="flex flex-col gap-2">
                <span className="text-label-lg font-semibold">알레르기 성분</span>
                <input value={form.allergies} onChange={(e) => set('allergies', e.target.value)} placeholder="쉼표로 구분 (예: 밀, 닭고기)" className={input} />
              </label>
            </div>
            <label className="flex items-center gap-2 text-label-lg">
              <input type="checkbox" checked={form.carrier} onChange={(e) => set('carrier', e.target.checked)} className="h-5 w-5 accent-primary-container" />
              이동장·유모차가 있어요
            </label>
          </fieldset>
        )}

        <div className="flex items-center justify-end gap-2 border-t border-outline-variant/30 pt-5">
          {serverError && <span role="alert" className="mr-auto text-label-sm text-error">{serverError}</span>}
          {isEdit && family?.myRole === 'OWNER' && (
            <button type="button" onClick={() => setConfirmDelete(true)} className="mr-auto flex items-center gap-1 text-label-md font-semibold text-error hover:underline">
              <Icon name="delete" className="text-[18px]" />
              반려견 삭제
            </button>
          )}
          <button type="button" onClick={() => navigate(-1)} className={ghostBtn}>
            취소
          </button>
          <button type="submit" disabled={saving} className={primaryBtn}>
            <Icon name="check" className="text-[20px]" />
            {isEdit ? '저장' : '등록하기'}
          </button>
        </div>
      </form>

      {confirmDelete && (
        <ConfirmModal
          title={`${form.name}의 정보를 삭제할까요?`}
          body="가족 모두에게서 사라지고 기록도 함께 지워져요. 되돌릴 수 없어요."
          confirmLabel="삭제"
          danger
          onConfirm={remove}
          onClose={() => setConfirmDelete(false)}
        />
      )}
    </div>
  )
}
