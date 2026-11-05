import { useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router'
import Icon from '../components/common/Icon'
import Photo from '../components/common/Photo'
import { card, chip, ghostBtn, input, primaryBtn } from '../components/common/styles'
import { DOG } from '../mocks/dog'

type Sex = 'MALE' | 'FEMALE'
interface Form {
  name: string
  breed: string
  weight: string
  birth: string // yyyy-MM
  sex: Sex | ''
  neutered: boolean | null
  memo: string
}

const BREEDS = ['말티즈', '푸들', '포메라니안', '시츄', '비숑 프리제', '진돗개', '골든 리트리버', '믹스']
const thisMonth = new Date().toISOString().slice(0, 7)
const EMPTY: Form = { name: '', breed: '', weight: '', birth: '', sex: '', neutered: null, memo: '' }
// 수정 화면 예시 값 (TODO: GET /api/dogs/{dogId})
const EDIT_SAMPLE: Form = { name: DOG.name, breed: DOG.breed, weight: String(DOG.weightKg), birth: '2023-05', sex: 'MALE', neutered: true, memo: '슬개골 주의' }

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

// 반려견 등록(/dogs/new)·수정(/dogs/:dogId/edit) 공용 폼: 필수 6개 + 건강 메모
export default function DogFormPage() {
  const { dogId } = useParams()
  const isEdit = Boolean(dogId)
  const navigate = useNavigate()
  const [form, setForm] = useState<Form>(isEdit ? EDIT_SAMPLE : EMPTY)
  const [submitted, setSubmitted] = useState(false)
  const set = <K extends keyof Form>(key: K, value: Form[K]) => setForm((f) => ({ ...f, [key]: value }))

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

  // TODO: POST /api/dogs 또는 PUT /api/dogs/{dogId}
  const submit = (e: FormEvent) => {
    e.preventDefault()
    setSubmitted(true)
    if (ok) navigate('/')
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
            <Photo src={isEdit ? DOG.photoUrl : undefined} alt="반려견 사진" className="h-24 w-24 rounded-full object-cover ring-4 ring-secondary-container" />
            <button type="button" aria-label="사진 바꾸기" className="absolute -right-1 -bottom-1 flex h-9 w-9 items-center justify-center rounded-full bg-primary-container text-on-primary shadow">
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
            <input value={form.breed} onChange={(e) => set('breed', e.target.value)} list="breeds" placeholder="검색하거나 직접 입력" className={input} />
            <datalist id="breeds">
              {BREEDS.map((b) => (
                <option key={b} value={b} />
              ))}
            </datalist>
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

        <div className="flex justify-end gap-2 border-t border-outline-variant/30 pt-5">
          <button type="button" onClick={() => navigate(-1)} className={ghostBtn}>
            취소
          </button>
          <button type="submit" className={primaryBtn}>
            <Icon name="check" className="text-[20px]" />
            {isEdit ? '저장' : '등록하기'}
          </button>
        </div>
      </form>
    </div>
  )
}
