// 백엔드 DTO와 같은 모양 (backend/.../dto). 화면용 모양으로 바꾸는 건 각 화면에서 합니다.

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

export type Role = 'USER' | 'ADMIN'
export type FamilyRole = 'OWNER' | 'EDITOR'
export type DogSize = 'SMALL' | 'MEDIUM' | 'LARGE'
export type Sex = 'MALE' | 'FEMALE'
export type ActivityLevel = 'LOW' | 'NORMAL' | 'HIGH'
export type RecordCategory = 'MEAL' | 'SNACK' | 'STOOL' | 'ACTIVITY' | 'SYMPTOM' | 'WEIGHT' | 'MEDICATION' | 'HOSPITAL' | 'QUESTION'
export type RecordSource = 'TEXT' | 'MANUAL' | 'COURSE' | 'VISIT'
export type StoolStatus = 'NORMAL' | 'SOFT' | 'DIARRHEA' | 'HARD' | 'BLOODY'
export type PlaceCategory = 'CAFE' | 'RESTAURANT' | 'PARK' | 'ETC'
export type VerdictLevel = 'OK' | 'CHECK' | 'HARD'
export type EditStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY'
export type ModelStatus = 'OK' | 'TIMEOUT' | 'INVALID' | 'ERROR'

export interface Me { id: number; nickname: string; provider: 'KAKAO' | 'NAVER' | 'DEV'; role: Role }

export interface Dog {
  id: number; name: string; breed: string; weightKg: number; size: DogSize; birthYm: string; ageMonths: number
  sex: Sex; neutered: boolean; hasCarrier: boolean; hasStroller: boolean; activityLevel: ActivityLevel | null
  diseases: string[]; allergies: string[]; foodKcalPerKg: number | null; photoUrl: string | null; memo: string | null
  createdBy: number
}
export interface DogInput {
  name: string; breed: string; weightKg: number; birthYm: string; sex: Sex; neutered: boolean
  hasCarrier?: boolean; hasStroller?: boolean; activityLevel?: ActivityLevel | null
  diseases?: string[]; allergies?: string[]; foodKcalPerKg?: number | null; memo?: string | null
}

export interface FamilyMemberItem { memberId: number; nickname: string; role: FamilyRole; joinedAt: string }
export interface Family { id: number; name: string; myRole: FamilyRole; members: FamilyMemberItem[]; dogCount: number; maxMembers: number }
export interface Invite { inviteCode: string; invitePath: string }
export interface InvitePreview { groupName: string; ownerNickname: string; memberCount: number; full: boolean }

export interface FoodItem { name: string; reason: string; howToFeed: string | null; sourceUrl: string | null; reasonTag: 'ALLERGY' | 'DISEASE' | null }
export interface NutritionCard {
  dogId: number; weightKg: number; lifeStage: string; rerKcal: number; factor: number; derKcal: number
  foodKcalPerKg: number | null; foodGrams: number; foodGramsEstimated: boolean; weightLoss: boolean; waterMinMl: number; waterMaxMl: number
  dangerFoods: FoodItem[]; safeFoods: FoodItem[]; cautionFoods: FoodItem[]
  diseaseCautions: { diseaseKey: string; label: string; cautionText: string; avoidKeywords: string[]; sourceUrl: string | null }[]
  unknownDiseases: string[]; notices: string[]
}

export interface RecordInput {
  category: RecordCategory; recordDate: string; memo?: string | null; data?: Record<string, unknown> | null
  weightKg?: number | null; stoolStatus?: StoolStatus | null; walkMinutes?: number | null; waterMl?: number | null
}
export interface HealthRecord {
  id: number; dogId: number; recordDate: string; category: RecordCategory; categoryLabel: string; memo: string | null
  data: Record<string, unknown>; weightKg: number | null; stoolStatus: StoolStatus | null; walkMinutes: number | null
  waterMl: number | null; source: RecordSource; createdBy: number; createdByNickname: string | null; createdAt: string
}
export interface DangerWarning { keyword: string; food: string; message: string }
export interface Parsed { candidates: RecordInput[]; warnings: DangerWarning[]; model: string }
export interface RecordSummary {
  from: string; to: string; byCategory: Partial<Record<RecordCategory, number>>
  byDate: { date: string; count: number; categories: RecordCategory[] }[]
  walkMinutesTotal: number; waterAvgMl: number | null; looseStoolCount: number
}
export interface WeightPoint { date: string; weightKg: number }

export interface VetQuestion { id: number; dogId: number; content: string; source: 'MEMO' | 'CONSULT'; resolved: boolean; createdBy: number; createdAt: string }

export interface Verdict { level: VerdictLevel; label: string; reasons: string[] }
export interface PlaceItem {
  id: number; name: string; category: PlaceCategory; categoryLabel: string; address: string | null
  lat: number; lng: number; distanceM: number | null; keywords: string[]; summaryFirstLine: string | null; verdict: Verdict | null
}
export interface PlaceCondition {
  key: string; label: string; value: string; tag: string | null; weightLimitKg: number | null
  evidenceCount: number; positiveCount: number; source: 'BLOG' | 'TOUR' | 'MANUAL' | 'ADMIN'
}
export interface PlaceDetail {
  id: number; name: string; category: PlaceCategory; categoryLabel: string; categoryName: string | null
  address: string | null; roadAddress: string | null; lat: number; lng: number; phone: string | null
  placeUrl: string | null; imageUrl: string | null; summary: string[]; keywords: string[]
  reviewSourceCount: number; reviewSummarizedAt: string | null; conditions: PlaceCondition[]
  sources: { url: string; title: string | null }[]; verdict: Verdict | null
}
export interface EditRequest {
  id: number; placeId: number; placeName: string | null; conditionKey: string; conditionLabel: string | null
  currentValue: string | null; requestedValue: string; comment: string | null; status: EditStatus
  rejectReason: string | null; memberId: number; nickname: string | null; createdAt: string
}

export interface ConsultAnswer {
  model: string; status: ModelStatus; urgency: Urgency | null; visitVet: boolean | null; answer: string | null
  reasons: string[]; homeCare: string[]; latencyMs: number
}
export interface Consultation {
  id: number; dogId: number; question: string; emergency: boolean; urgency: Urgency; urgencyLabel: string
  visitVet: boolean; crossChecked: boolean; answeredCount: number; totalModels: number; summary: string[]
  notices: string[]; answers: ConsultAnswer[]; createdBy: number; createdAt: string
  contextLines: string[]; archivedAt: string | null
}
export interface ConsultItem { id: number; question: string; urgency: Urgency; urgencyLabel: string; emergency: boolean; visitVet: boolean; createdAt: string; archivedAt: string | null }

export interface CourseStop {
  seq: number; placeId: number; name: string; category: PlaceCategory; categoryLabel: string
  lat: number; lng: number; keywords: string[]; legDistanceM: number; legMinutes: number
}
export interface Course {
  id: number; dogId: number; stops: CourseStop[]; legs: { from: number; to: number; distanceM: number; minutes: number }[]
  startToFirstM: number; totalDistanceM: number; totalMinutes: number; reasons: string[]
  visitedAt: string | null; savedAt: string | null; createdBy: number; createdAt: string
}

export interface ReportRow { date: string; category: string; memo: string; by: string | null }
export interface ReportData {
  basic: { name: string; breed: string; sex: string; neutered: boolean; age: string; weightKg: number; size: string; diseases: string[]; allergies: string[] }
  from: string; to: string; generatedAt: string; summary: string[]; summaryModel: string | null; signals: string[]
  meals: ReportRow[]; stoolCounts: Record<string, number>; stools: ReportRow[]; symptoms: ReportRow[]
  walkMinutesTotal: number; activities: ReportRow[]; weights: WeightPoint[]; medical: ReportRow[]
  questions: { content: string; askedOn: string | null; resolved: boolean }[]
}

// 관리자 회원 현황: 입력 내용 없이 개수·마지막 입력일만
export interface AdminDataCount { key: string; label: string; count: number; lastAt: string | null }
export interface AdminMemberRow {
  id: number; nickname: string; provider: Me['provider']; role: Role; joinedAt: string
  withdrawn: boolean; totalCount: number; lastActivityAt: string | null
}
export interface AdminMemberList { memberCount: number; activeCount: number; withdrawnCount: number; members: AdminMemberRow[] }
export interface AdminMemberDetail {
  member: AdminMemberRow; familyName: string | null; familyRole: FamilyRole | null; familySize: number | null; counts: AdminDataCount[]
}
