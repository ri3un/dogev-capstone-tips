# 프론트 ↔ 백엔드 연결 진행 기록 (이어하기용)

> 세션이 끊기면 이 파일부터 읽고, 아래 "남은 일" 체크리스트의 첫 번째 [ ] 부터 이어서 합니다.
> 화면 하나를 끝낼 때마다 이 파일의 체크를 갱신하고 커밋합니다.

## 이어하기 문장 (새 세션에 그대로 붙여 넣기)

```
docs/superpowers/plans/2026-11-08-frontend-connect.md 를 읽고 남은 일을 이어서 해 줘.
브랜치는 feature/connect-api. 디자인(className)은 바꾸지 말고 목업만 실제 API로 바꿔.
```

## 상태 요약 (2026-11-08)

- 브랜치: `feature/connect-api` = `feature/more-pages-3`(최신 프론트, 74fddda) + `feature/backend-core`(백엔드) 머지(4af0a3d). **push 안 함**
- 백엔드: 완료. 테스트 111개 통과. 문서 `백엔드_설계_컨텍스트.md` 7장, 계획 `docs/superpowers/plans/2026-11-08-backend-core.md`
- 로컬 MySQL 8.0(서비스 이름 `MySQL`), root 비밀번호는 `.env`의 `DB_ROOT_PASSWORD`. DB `gaebalhagae`(점검용 데이터 조금 있음)

## 실행·검증 방법

```
cd backend && ./gradlew bootRun          # 8080, local 프로필(dev-login 있음)
cd frontend && npm run dev               # 5173, /api 는 8080 으로 프록시
cd frontend && npm run lint && npm run build   # 프론트 검증 (테스트 도구 없음)
cd backend && ./gradlew test             # 백엔드 테스트
```
로그인: 카카오·네이버 키가 없어서 개발 모드 로그인 화면의 "개발용 로그인"(닉네임 입력, `POST /api/auth/dev-login`)으로 들어감.

## 규칙

- 디자인(className·레이아웃) 그대로. 목업 import 를 API 호출로만 바꿈
- API 호출은 `src/api/endpoints.ts`, 타입은 `src/api/types.ts`(백엔드 DTO 그대로), 날짜 등은 `src/api/format.ts`
- 데이터 불러오기 `src/hooks/useApi.ts`, 지금 반려견 `src/hooks/useCurrentDog.ts`(localStorage `dogId`)
- 반려견이 없으면 `/onboarding` 으로 보냄
- 날짜는 `todayStr()`(현지 시간). `toISOString()` 쓰지 않기(한국 오전 9시 전 어제 날짜 버그)
- 백엔드 값 → 화면 값 변환은 화면 파일 안의 작은 함수로
- 커밋: `feat: 한국어 요약` + `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## 완료

- [x] 브랜치 생성·머지, `.gitignore` 충돌 해결
- [x] `api/client.ts`(patch·upload·blob, 401 시 토큰 삭제), `api/types.ts`, `api/endpoints.ts`, `api/format.ts`
- [x] `hooks/useApi.ts`, `hooks/useCurrentDog.ts`

## 남은 일 (위에서부터)

- [x] LoginPage: 개발 모드 전용 "개발용 로그인" 추가 / OAuthCallbackPage: `api/session.ts` finishLogin(반려견 없으면 /onboarding)
- [x] AppHeader: `authApi.me`, `adminApi.requests('PENDING')`, 반려견 수정 링크 실제 id, 로그아웃 시 토큰 삭제
- [x] OnboardingPage(변경 불필요), DogFormPage(등록·수정·사진·삭제 연결, 백엔드 dog.memo 컬럼 추가)
- [x] MyPage (DOG·FAMILY·ME·EDIT_REQUESTS 목업 → useCurrentDog, familyApi.get, authApi.me, adminApi.requests, 로그아웃은 api/session logout)
- [x] HomePage (DOG, MONTHLY_SUMMARY → dog + nutrition + records summary)
- [x] FamilyPage, InvitePage (familyApi, 이슈 3·6의 가족 메뉴 부분 해결)
- [x] RecordsPage(달력·체중 그래프·달성률), RecordNewPage(글 → 후보 → batch 저장, 홈에서 넘긴 글 자동 변환, `?kind=` 읽기, 503이면 직접 입력) — 이슈 1·4·5(홈) 해결
- [x] QuestionsPage (questionApi, 작성자는 가족 구성원 닉네임, 날짜는 현지 시간)
- [x] NutritionPage (dogApi.nutrition — 서버는 열량·사료량을 범위 아닌 한 값으로 줌, 주의 음식 중 알레르기는 빨간 배지)
- [x] MapPage, PlaceDrawer, SourceModal, EditRequestModal (placeApi. 상수는 `constants/place.ts`(서버 키), 위치는 `hooks/useMyLocation.ts`. 핀은 위·경도 범위를 % 로 펼친 임시 위치, 카테고리 칩은 카페·식당·공원·산책로 3개)
- [x] MyEditRequestsPage, AdminEditRequestsPage (placeApi.myRequests, adminApi — 값은 valueLabel 로 한글 표시, 이슈 2 반려 모달 X 닫기 시 사유 지움)
- [x] ConsultPage (consultApi, 질문 메모 저장, /consult/:id 로 지난 상담 열기, 검색은 지난 상담·질문 메모에서 글자 포함 — 이슈 5 상담 버튼 해결)
- [x] CoursePage, CoursePlanPage, CoursesPage (courseApi, 위치는 useMyLocation. 코스 화면 카드는 최근 추천 코스 3개, 테마 칩은 `/course/plan?theme=` 로 들를 곳 순서 미리 채움. 다시 추천은 로딩 중 비활성 — 이슈 6 해결. 산책 팁(WALK_TIP)은 날씨 API 없어 아직 목업)
- [x] ReportPage (reportApi.preview, PDF 다운로드, 날짜 todayStr — 이슈 1 해결. 섹션마다 최근 6건만, 이상 신호는 글자로 배변·체중·증상 섹션에 붙임)
- [ ] mocks 폴더 정리: 화면 상수(KIND_DOT·KIND_BADGE·KIND_LABEL·PARSED_LABEL·PARSED_OPTIONS·EXAMPLE_SENTENCES, CONDITIONS 등)는 `src/constants/` 로 옮기고 나머지 목업 삭제
- [ ] lint·build 통과 → 브라우저로 주요 흐름 확인(백엔드+프론트 실행) → 커밋
- [x] Claude 앱으로 작업 정보 옮기기: Claude Docs 문서 https://claude.ai/artifact/5H1TfDmuuvFs41YZzmLDsZ (끝나면 "프론트 연결 진행 상황" 체크 갱신). 프로젝트에 넣으려면 사용자가 `/send-to-project` 직접 실행

## 백엔드 연결 때 같이 고치기로 한 프론트 이슈 (메모리에서)

1. `toISOString()` UTC 날짜 → `todayStr()` 로 (QuestionsPage, ReportPage, RecordNewPage, DogFormPage)
2. AdminEditRequestsPage 반려 모달 X로 닫아도 사유 텍스트 남음
3. FamilyPage 복사: 클립보드 실패해도 '복사됨' 표시
4. RecordsPage `/records/new?kind=weight` 의 kind 를 RecordNewPage 가 안 읽음
5. 홈 '기록으로 바꾸기', 상담 '3개 AI에게 물어보기' 버튼 동작 없음 → API 연결로 해결
6. FamilyPage 구성원 메뉴 바깥 클릭 시 안 닫힘, CoursePlan '다시 추천'이 로딩 중에도 눌림

## 백엔드와 화면 값 대응 메모

| 화면(목업) | 백엔드 |
|---|---|
| 장소 category cafe/restaurant/trail/park | CAFE/RESTAURANT/PARK/ETC (trail·park → PARK) |
| PetStatus OK/NEEDS_CHECK/HARD | verdict.level OK/CHECK/HARD |
| 조건 key size/space/carrier/leash/manner/seat/offleash/water/parking | size_allowed/indoor/carrier_required/leash_required/manner_belt_required/seat_allowed/off_leash_area/water_or_pet_menu/parking |
| 기록 kind meal/poop/activity/symptom/weight/medicine/question | MEAL(+SNACK)/STOOL/ACTIVITY/SYMPTOM/WEIGHT/MEDICATION(+HOSPITAL)/QUESTION |
| 초대 상태 OK/ALONE_WITH_DOGS/WITH_OTHERS/... | preview(memberCount, full) + accept 오류 코드 FAMILY_FULL/FAMILY_ALREADY_JOINED/INVITE_NOT_FOUND |
