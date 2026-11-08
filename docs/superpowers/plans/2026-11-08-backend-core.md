# 킁킁 백엔드 코어 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (이번에는 같은 세션에서 인라인 실행). 각 Task는 TDD(실패 테스트 → 구현 → 통과 → 커밋) 순서.

**Goal:** `백엔드_설계_컨텍스트.md` 7장의 API 전부(인증·가족·반려견·영양·기록·질문 메모·장소·수정 요청·상담·코스·리포트·사진)를 `feature/backend-core`에 구현한다.

**Architecture:** 기능별 패키지(`controller / service / repository / domain / dto`), 공통 응답 `ApiResponse`, 오류는 `ErrorCode` + `BusinessException`. 가족 그룹이 반려견의 주인이며 권한 확인은 `FamilyAccessService` 한 곳. LLM은 기존 `com.gaebalhagae.llm.LlmClient`를 재사용하고 `LlmFallbackCaller`(기록 변환·요약), `LlmParallelCaller`(상담)를 추가.

**Tech Stack:** Java 17, Spring Boot 3.5.16, JPA(Hibernate 6), MySQL 8.4(운영)·H2(테스트·collect), Spring Security + OAuth2 Client + jjwt 0.12.6, Flying Saucer(OpenPDF) + Thymeleaf + JFreeChart, springdoc.

**Spec:** `백엔드_설계_컨텍스트.md` (저장소 루트). 결정 근거는 Claude Docs "졸업작품 종합 보고서".

## Global Constraints

- 응답 `{success, data, error:{code, message}}`, LLM 전부 실패 시 503 `LLM_UNAVAILABLE`
- 테이블: 소문자 단수 snake_case, PK `id`, 모든 테이블 `created_at`·`updated_at`
- 남의 반려견은 403이 아니라 **404**. JWT에 그룹 정보 넣지 않음
- 코스 출발 좌표 **저장 안 함**
- 위험 음식 경고는 LLM이 아니라 서버 규칙
- LLM에 개인 식별 정보(이름·닉네임·가족 정보) 미전송, 상담 LLM 대기 중 DB 트랜잭션 잡지 않음
- 키는 `${ENV}`로만, `.env` 커밋 금지, push 금지(로컬 커밋만)
- 커밋: `feat: 한국어 요약`
- 설계 차별점 9개(RAG 등)는 범위 밖

## 검증 명령

`cd backend && ./gradlew test` (Windows Git Bash에서도 동일). 테스트 DB는 H2(MySQL 모드).

---

### Task 1: 공통 기반 (JPA 감사, H2, 의존성, ErrorCode)

**Files:**
- Modify: `backend/build.gradle` (oauth2-client, jjwt 0.12.6, thymeleaf, flying-saucer-pdf, jfreechart, h2)
- Modify: `backend/src/main/resources/application.yml` (collect → H2 메모리, jwt·oauth2·openrouter·upload 설정)
- Create: `global/config/JpaConfig.java` (`@EnableJpaAuditing`), `global/domain/BaseTimeEntity.java`
- Create: `src/test/resources/application-test.yml` (H2 MySQL 모드)
- Modify: `global/response/ErrorCode.java` (DOG_NOT_FOUND, FAMILY_NOT_FOUND, FAMILY_OWNER_ONLY, FAMILY_FULL, FAMILY_OWNER_MUST_TRANSFER, INVALID_FILE 등)

**Produces:** `BaseTimeEntity`(createdAt, updatedAt), 테스트 프로필 `test`.

- [ ] collect 프로필 컨텍스트 테스트가 H2로 계속 뜨는지 확인 (기존 `CollectProfileContextTest`)
- [ ] 커밋

### Task 2: 인증 (member, JWT, OAuth2, dev-login)

**Files:**
- Create: `member/domain/{Member, Provider, Role}.java`, `member/repository/MemberRepository.java`
- Create: `member/service/MemberService.java` (`Member login(Provider, providerId, nickname)` → 없으면 생성 + `FamilyService.ensurePersonalGroup`)
- Create: `member/controller/{AuthController(dev-login, @Profile("local","test")), MemberController(/api/members/me)}`
- Create: `global/security/{JwtProvider, JwtAuthFilter, CustomOAuth2UserService, OAuth2SuccessHandler, LoginMember}.java`
- Modify: `global/config/SecurityConfig.java`
- Test: `JwtProviderTest`, `AuthApiTest`(SpringBootTest + MockMvc)

**Produces:** 컨트롤러에서 `@AuthenticationPrincipal LoginMember me` → `me.id()`, `me.role()`. `/api/admin/**`는 `ROLE_ADMIN`.

테스트:
- 토큰 발급 → 파싱 시 memberId·role 복원, 위조·만료 토큰은 인증 안 됨
- dev-login → `/api/members/me` 200, 토큰 없으면 401 공통 오류
- 같은 provider+providerId로 두 번 로그인해도 회원 1명

### Task 3: 가족 그룹 + 반려견 + FamilyAccessService

**Files:**
- Create: `family/domain/{FamilyGroup, FamilyMember, FamilyRole}`, `family/repository/*`, `family/service/{FamilyService, FamilyAccessService, InviteCodeGenerator}`, `family/controller/FamilyController`, `family/dto/*`
- Create: `dog/domain/{Dog, DogSize, Sex, ActivityLevel}`, `dog/repository/DogRepository`, `dog/service/DogService`, `dog/controller/DogController`, `dog/dto/*`
- Test: `DogSizeTest`, `FamilyApiTest`, `DogApiTest`

**Interfaces (Produces):**
- `FamilyAccessService.checkDog(Long memberId, Long dogId): Dog` — 내 그룹 반려견 아니면 `DOG_NOT_FOUND`(404)
- `FamilyAccessService.checkDogOwner(Long memberId, Long dogId): Dog` — 관리자 아니면 `FAMILY_OWNER_ONLY`(403)
- `FamilyAccessService.requireOwner(Long memberId): FamilyMember`
- `FamilyService.ensurePersonalGroup(Long memberId)`
- `DogSize.of(BigDecimal kg)`: <10 SMALL, <25 MEDIUM, 그 외 LARGE

정책 테스트:
- 로그인하면 개인 그룹(OWNER) 생김, 다시 로그인해도 1개
- 초대 수락: 혼자인 그룹의 반려견이 새 그룹으로 이동, 옛 그룹 삭제. 6명이면 `FAMILY_FULL`
- 다른 가족이 있는 그룹에 속해 있으면 수락 불가(`FAMILY_ALREADY_JOINED`)
- 나가기·내보내기: 반려견은 남고, 나간 사람은 새 개인 그룹. 관리자는 다른 가족이 있으면 나가기 불가(`FAMILY_OWNER_MUST_TRANSFER`)
- 초대 링크 새로 만들기 → 옛 코드로 수락 시 404
- 남의 반려견 GET → 404, EDITOR가 반려견 삭제 → 403

### Task 4: 영양 카드 (food_guide, disease_guide, RER)

**Files:**
- Create: `dog/domain/{FoodGuide, FoodType, DiseaseGuide}`, `dog/repository/{FoodGuideRepository, DiseaseGuideRepository}`, `dog/service/{NutritionService, NutritionCalculator, GuideDataLoader}`, `dog/dto/NutritionCardResponse`
- Test: `NutritionCalculatorTest`, `NutritionApiTest`

규칙: RER = 70 × kg^0.75. 계수: 4개월 미만 3.0, 1살 미만 2.0, 7살 이상 1.4, 중성화 1.6, 아니면 1.8. 사료량(g) = DER ÷ 사료 kcal/kg × 1000(사료 칼로리 없으면 null). 음수량은 kg당 30~60ml 범위. 지병이 있으면 맨 위에 "수의사와 상의" 문구. 고정 데이터는 빈 테이블일 때 `GuideDataLoader`가 넣음(ASPCA 출처).

### Task 5: 건강 기록 + 글 → 기록 + 질문 메모

**Files:**
- Create: `llm/{OpenRouterClient, LlmFallbackCaller, LlmJson}.java`
- Create: `record/domain/{HealthRecord, RecordCategory, RecordSource}`, `record/repository`, `record/service/{RecordService, RecordParseService, DangerFoodRule}`, `record/controller/RecordController`, `record/dto/*`
- Create: `question/domain/{VetQuestion, QuestionSource}`, `question/repository`, `question/service`, `question/controller`
- Test: `DangerFoodRuleTest`, `RecordParseServiceTest`(가짜 LLM), `RecordApiTest`

**Interfaces:**
- `LlmFallbackCaller.call(String system, String user, Function<String,T> validator): T` — Groq → Gemini 순, 같은 모델 형식 오류 1회 재요청, 전부 실패 시 `LLM_UNAVAILABLE`
- `RecordService.createActivity(Long dogId, Long memberId, LocalDate date, RecordSource source, int walkMinutes, Map<String,Object> data)` — 코스·방문이 사용

카테고리 9개: MEAL, SNACK, STOOL, ACTIVITY, SYMPTOM, WEIGHT, MEDICATION(투약·접종), HOSPITAL, QUESTION. QUESTION 후보는 저장 시 vet_question(MEMO)으로 들어감.

### Task 6: 장소 API + import + 방문 + 수정 요청

**Files:**
- Create: `place/domain/{Place, PlaceCategory, PlaceCondition, ConditionSource, PlaceSource, PlaceEditRequest, EditRequestStatus}`, `place/repository/*`
- Create: `place/service/{PlaceQueryService, PlaceVerdict, PlaceImportService, PlaceVisitService, EditRequestService}`, `place/controller/{PlaceController, AdminPlaceController, EditRequestController}`, `place/dto/*`
- Test: `PlaceVerdictTest`, `PlaceImportServiceTest`, `PlaceApiTest`, `EditRequestApiTest`

규칙:
- import 입력 = 수집 JSON(`CollectedPlace` 목록 + `ReviewSummary` 목록). kakaoId로 upsert, 요약은 장소 이름으로 매칭. ADMIN·MANUAL 출처 조건은 BLOG가 덮어쓰지 않음 (ADMIN > MANUAL > TOUR > BLOG)
- 카테고리: CE7 → CAFE, FD6 → RESTAURANT, AT4 또는 이름에 공원·산책로 → PARK, 그 외 ETC
- 판정(dogId 있을 때): 크기 불가 → HARD, 크기 unknown/NEEDS_CHECK → CHECK, 이동장 필요 + 이동장·유모차 없음 → CHECK, 그 외 OK
- 승인 시 해당 조건을 source=ADMIN으로 반영, 반려 시 사유만 남기고 장소 그대로

### Task 7: AI 상담

**Files:**
- Create: `llm/LlmParallelCaller.java`, `consult/domain/{Consultation, ConsultationAnswer, AnswerStatus, Urgency}`, `consult/repository`, `consult/service/{ConsultService, EmergencyKeywords, ConsultAggregator}`, `consult/controller`, `consult/dto`
- Test: `LlmParallelCallerTest`, `ConsultAggregatorTest`, `ConsultApiTest`(가짜 LLM)

규칙: 3개 병렬, 모델별 15초. 형식 오류면 같은 모델 1회 재요청, 또 틀리면 INVALID. OK 0개 → 503, 1개 → "교차검증 안 됨". 긴급도는 가장 높은 값, 병원 권고는 하나라도 있으면. 응급 키워드면 맨 앞에 "즉시 동물병원". 모든 응답에 "참고 정보이며 진단이 아님". LLM 호출 동안 트랜잭션 없음. 프롬프트에 반려견 이름 없음(견종·나이·몸무게·지병만).

### Task 8: 코스 추천 + 다녀왔어요

**Files:**
- Create: `course/domain/{Course, CoursePlace}`, `course/repository`, `course/service/{CourseService, CourseRules, GreedyCourseBuilder, Geo}`, `course/controller`, `course/dto`
- Test: `GeoTest`, `CourseRulesTest`, `GreedyCourseBuilderTest`, `CourseApiTest`

규칙 B 4개: 8세 이상(구간 1km·총 2km) / 관절 지병 또는 14일 내 증상에 "절뚝"(총 2km, 오프리쉬 제외) / 3일 내 설사·무른 변 2회 이상(식당 → 산책로·카페, 총 1.5km) / 활동량 높음 + 지병 없음(오프리쉬 우선). 이유 한 줄씩. 시속 4km. 후보 부족 시 반경 1→3→5km 확대, 그래도 없으면 `COURSE_NOT_ENOUGH_PLACES`. 완료 시 ACTIVITY 기록(source COURSE).

### Task 9: PDF 리포트

**Files:**
- Create: `report/service/{ReportService, ReportSignals, WeightChart, PdfRenderer}`, `report/controller`, `report/dto/ReportData`, `resources/templates/report.html`, `resources/fonts/*.ttf`
- Test: `ReportSignalsTest`, `ReportApiTest`(PDF 바이트가 `%PDF`로 시작)

섹션 9개(가정, 팀 확인 필요): ①기본 정보 ②기간 요약(LLM 3줄, 실패 시 규칙 문장) ③식사·간식 ④배변 ⑤증상 ⑥활동(산책·다녀왔어요) ⑦체중(그래프+표) ⑧투약·접종·병원 ⑨질문 메모. 기간 기본 30일·최대 92일. 이상 신호 규칙 3개: 기간 내 체중 5% 이상 변화 / 3일 안 설사·무른 변 2회 이상 / 증상 기록 3회 이상.

### Task 10: 반려견 사진 업로드

**Files:**
- Create: `dog/service/PhotoStorage.java`, `DogController`에 `POST /api/dogs/{dogId}/photo`
- Modify: `global/config/WebConfig.java` (`/uploads/**` 정적 제공, local용)
- Test: `PhotoApiTest`

규칙: JPEG·PNG·WEBP만(매직 바이트 확인), 5MB 이하, 파일 이름은 UUID, 저장 경로 `${UPLOAD_DIR:./uploads}`.

### Task 11: 마무리 검증

- [ ] `./gradlew test` 전체 통과
- [ ] `.env.example`에 `OPENROUTER_MODEL`, `UPLOAD_DIR` 추가
- [ ] `백엔드_설계_컨텍스트.md` 7장 상태표 갱신
- [ ] 코드 리뷰(보안·JPA) 후 로컬 커밋
