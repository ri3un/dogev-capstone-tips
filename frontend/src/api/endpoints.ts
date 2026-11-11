import { api } from './client'
import type {
  AdminMemberDetail, AdminMemberList, Consultation, ConsultItem, Course, Dog, DogInput, EditRequest, EditStatus, Family, HealthRecord, Invite,
  InvitePreview, Me, NutritionCard, Parsed, PlaceCategory, PlaceDetail, PlaceItem, RecordCategory, RecordInput,
  RecordSummary, ReportData, VetQuestion, WeightPoint,
} from './types'

// 백엔드 API 목록 (백엔드_설계_컨텍스트.md 7장)
export const authApi = {
  devLogin: (nickname: string, admin = false) =>
    api.post<{ token: string; member: Me }>('/auth/dev-login', { nickname, admin }),
  me: () => api.get<Me>('/members/me'),
  withdraw: () => api.delete<void>('/members/me'),
}

export const dogApi = {
  list: () => api.get<Dog[]>('/dogs'),
  get: (id: number) => api.get<Dog>(`/dogs/${id}`),
  create: (body: DogInput) => api.post<Dog>('/dogs', body),
  update: (id: number, body: DogInput) => api.patch<Dog>(`/dogs/${id}`, body),
  remove: (id: number) => api.delete<void>(`/dogs/${id}`),
  photo: (id: number, file: File) => api.upload<Dog>(`/dogs/${id}/photo`, file),
  nutrition: (id: number) => api.get<NutritionCard>(`/dogs/${id}/nutrition-card`),
}

export const familyApi = {
  get: () => api.get<Family>('/family'),
  rename: (name: string) => api.patch<Family>('/family', { name }),
  invite: () => api.get<Invite>('/family/invite'),
  resetInvite: () => api.post<Invite>('/family/invite/reset'),
  preview: (code: string) => api.get<InvitePreview>(`/family/invites/${code}`),
  accept: (code: string) => api.post<Family>(`/family/invites/${code}/accept`),
  kick: (memberId: number) => api.delete<void>(`/family/members/${memberId}`),
  transferOwner: (memberId: number) => api.post<void>('/family/owner', { memberId }),
  leave: () => api.post<void>('/family/leave'),
}

type Range = { from?: string; to?: string }
export const recordApi = {
  parse: (dogId: number, text: string, date?: string) => api.post<Parsed>(`/dogs/${dogId}/records/parse`, { text, date }),
  batch: (dogId: number, records: RecordInput[]) => api.post<HealthRecord[]>(`/dogs/${dogId}/records/batch`, { records }),
  create: (dogId: number, body: RecordInput) => api.post<HealthRecord>(`/dogs/${dogId}/records`, body),
  list: (dogId: number, params: Range & { category?: RecordCategory } = {}) =>
    api.get<HealthRecord[]>(`/dogs/${dogId}/records`, params),
  summary: (dogId: number, params: Range = {}) => api.get<RecordSummary>(`/dogs/${dogId}/records/summary`, params),
  weights: (dogId: number, params: Range = {}) => api.get<WeightPoint[]>(`/dogs/${dogId}/records/weights`, params),
  update: (id: number, body: RecordInput) => api.put<HealthRecord>(`/records/${id}`, body),
  remove: (id: number) => api.delete<void>(`/records/${id}`),
}

export const questionApi = {
  list: (dogId: number, resolved?: boolean) => api.get<VetQuestion[]>(`/dogs/${dogId}/vet-questions`, { resolved }),
  create: (dogId: number, content: string, source: 'MEMO' | 'CONSULT' = 'MEMO') =>
    api.post<VetQuestion>(`/dogs/${dogId}/vet-questions`, { content, source }),
  edit: (id: number, body: { content?: string; resolved?: boolean }) => api.patch<VetQuestion>(`/vet-questions/${id}`, body),
  remove: (id: number) => api.delete<void>(`/vet-questions/${id}`),
}

export interface PlaceQuery {
  category?: PlaceCategory; q?: string; lat?: number; lng?: number; radiusKm?: number
  dogId?: number; fitOnly?: boolean; conditions?: string[]
}
export const placeApi = {
  list: (p: PlaceQuery = {}) => api.get<PlaceItem[]>('/places', { ...p, conditions: p.conditions?.join(',') || undefined }),
  get: (id: number, dogId?: number) => api.get<PlaceDetail>(`/places/${id}`, { dogId }),
  visit: (id: number, dogId: number) => api.post<HealthRecord>(`/places/${id}/visits`, { dogId }),
  requestEdit: (id: number, conditionKey: string, requestedValue: string, comment?: string) =>
    api.post<EditRequest>(`/places/${id}/edit-requests`, { conditionKey, requestedValue, comment }),
  myRequests: () => api.get<EditRequest[]>('/me/edit-requests'),
}

export const adminApi = {
  requests: (status?: EditStatus) => api.get<EditRequest[]>('/admin/edit-requests', { status }),
  approve: (id: number) => api.post<EditRequest>(`/admin/edit-requests/${id}/approve`),
  reject: (id: number, reason: string) => api.post<EditRequest>(`/admin/edit-requests/${id}/reject`, { reason }),
  members: () => api.get<AdminMemberList>('/admin/members'),
  member: (id: number) => api.get<AdminMemberDetail>(`/admin/members/${id}`),
}

export const consultApi = {
  ask: (dogId: number, question: string) => api.post<Consultation>(`/dogs/${dogId}/consultations`, { question }),
  list: (dogId: number) => api.get<ConsultItem[]>(`/dogs/${dogId}/consultations`),
  get: (id: number) => api.get<Consultation>(`/consultations/${id}`),
  remove: (id: number) => api.delete<void>(`/consultations/${id}`),
}

export const courseApi = {
  recommend: (body: { dogId: number; lat: number; lng: number; placeTypes: PlaceCategory[]; radiusKm?: number }) =>
    api.post<Course>('/courses/recommend', body),
  get: (id: number) => api.get<Course>(`/courses/${id}`),
  list: (dogId: number) => api.get<Course[]>(`/dogs/${dogId}/courses`),
  complete: (id: number) => api.post<Course>(`/courses/${id}/complete`),
}

export const reportApi = {
  preview: (dogId: number, params: Range = {}) => api.get<ReportData>(`/dogs/${dogId}/reports/preview`, params),
  pdf: (dogId: number, params: Range = {}) => api.blob(`/dogs/${dogId}/reports/pdf`, params),
}
