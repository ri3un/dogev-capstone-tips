# 킁킁 수정사항 8개 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## ▶ 이어하기 (한도로 멈췄다가 다시 시작할 때 이 문장을 그대로 Claude 에게)

> "`docs/superpowers/plans/2026-11-11-kungkung-changes.md` 계획을 이어서 해 줘. 브랜치 `feature/kungkung-changes` 에서 `git log --oneline -5` 로 마지막 커밋을 보고, 체크 안 된 첫 Task 부터 executing-plans 스킬로 진행해."

- 진행 규칙: Task 하나 끝날 때마다 이 파일의 체크박스를 [x]로 바꾸고 **코드와 함께 커밋**. 그러면 커밋 기록 = 진행 상황.
- 커밋은 한국어 `feat:/fix:/docs:`. push 는 사용자에게 묻고 나서.
- 백엔드 검증: `cd backend && ./gradlew test` (H2). 프론트 검증: `cd frontend && npm run lint && npm run build`.
- 프론트 디자인(className)은 home-design(펌킨 스파이스)을 따름. 새 요소는 주변 요소의 className 을 복사.

**Goal:** 확정된 수정사항 8개(사양 파일)를 home-design 프론트 위에서 백엔드+프론트로 구현.

**Architecture:** 기존 Spring Boot 패키지(course·dog·consult·record) 안에서 작은 추가만. LLM 호출은 새로 늘리지 않음(5번 상담만 기존 그대로). 프론트는 React 19 + 기존 `api/endpoints.ts`·`types.ts`.

**Tech Stack:** Java 17, Spring Boot, JPA(ddl-auto update), JUnit5 + MockMvc(`ApiTestSupport`), React 19 + Vite + Tailwind 4.

**Spec:** `docs/superpowers/specs/2026-11-10-kungkung-changes.md`

## Global Constraints
- 테스트 이름은 한국어(저장소 관례).
- 권한 없는 리소스는 기존처럼 404(`*_NOT_FOUND`)로 숨김.
- LLM 프롬프트에 반려견 이름·보호자 정보·기록 원문 넣지 않음.
- 새 스케줄러 시각 04:00 Asia/Seoul (장소 수집 03:00과 안 겹치게).
- 사료 기본 칼로리 3,500kcal/kg, 비만 계수 1.0, 상담 보관 90일, 상담 맥락 14일.

---

### Task 0: 브랜치 + 사양·계획 커밋
- [x] `git checkout -b feature/kungkung-changes feature/home-design`
- [x] 사양·계획 파일 커밋 `docs: 수정사항 8개 사양과 구현 계획`

### Task 1: 코스 삭제 API (항목 1)
**Files:** Modify `backend/.../course/controller/CourseController.java`, `course/service/CourseService.java`, `course/repository/CoursePlaceRepository.java`; Test `backend/src/test/java/com/gaebalhagae/course/CourseApiTest.java`
**Produces:** `DELETE /api/courses/{courseId}` → `ApiResponse<Void>`

- [x] Step 1: 실패 테스트 추가
```java
@Test
void 코스를_삭제해도_다녀온_활동_기록은_남는다() throws Exception {
    postApi(login("관리자", true), "/api/admin/places/import", PlaceApiTest.sample());
    String user = login("다인");
    long dogId = createDog(user, "콩이", "4", "2022-01");
    Number id = read(postApi(user, "/api/courses/recommend", body(dogId)), "$.data.id");
    postApi(user, "/api/courses/{id}/complete", null, id);

    deleteApi(login("남"), "/api/courses/{id}", id).andExpect(status().isNotFound());
    deleteApi(user, "/api/courses/{id}", id).andExpect(status().isOk());

    getApi(user, "/api/courses/{id}", id).andExpect(status().isNotFound());
    getApi(user, "/api/dogs/{id}/courses", dogId).andExpect(jsonPath("$.data", hasSize(0)));
    getApi(user, "/api/dogs/{id}/records?category=ACTIVITY", dogId).andExpect(jsonPath("$.data", hasSize(1)));
}
```
- [x] Step 2: `./gradlew test --tests '*CourseApiTest'` → 실패(405/404) 확인
- [x] Step 3: 구현
  - `CoursePlaceRepository`: `@Modifying @Query("delete from CoursePlace p where p.courseId = :courseId") void deleteByCourseId(@Param("courseId") Long courseId);`
  - `CourseService.delete(Long memberId, Long courseId)`: `Course c = find(memberId, courseId); coursePlaceRepository.deleteByCourseId(c.getId()); courseRepository.delete(c);`
  - `CourseController`: `@DeleteMapping("/api/courses/{courseId}")` → `courseService.delete(me.id(), courseId)` (ConsultController 의 delete 와 같은 모양으로)
- [x] Step 4: 테스트 통과 확인
- [x] Step 5: 커밋 `feat: 추천받은 코스 삭제 API`

### Task 2: 질병 맞춤 영양 + 사료량 g (항목 2·7)
**Files:** Modify `dog/domain/FoodGuide.java`, `dog/service/GuideDataLoader.java`, `dog/service/NutritionCalculator.java`, `dog/service/NutritionService.java`, `dog/dto/NutritionCardResponse.java`; Test: 기존 영양 테스트(없으면 `backend/src/test/java/com/gaebalhagae/dog/NutritionApiTest.java`, `NutritionCalculatorTest.java` 생성)
**Produces (응답 필드 추가):** `foodGrams`(항상 값), `foodGramsEstimated: boolean`, `weightLoss: boolean`(비만 감량 기준), `FoodItem.reasonTag: "ALLERGY"|"DISEASE"|null`, `unknownDiseases: List<String>`(등록 안내 없는 자유 입력 지병)

- [x] Step 1: 실패 테스트
  - `NutritionCalculator.calc(10, 36, true, null, true)` → factor 1.0, foodGrams = round(RER(10)*1.0/3500*1000), estimated true
  - `calc(10, 36, true, null, false)` → factor 1.6, foodGrams 값 있음
  - API: 지병 OBESITY 견 → `cautionFoods` 에 고구마(reasonTag DISEASE, reason "비만이 있어 조금만 주세요"), `factor` 1.0, `weightLoss` true. 지병 없음 → 고구마 safe. 지병 "갑상선" → `unknownDiseases` ["갑상선"], notices 에 "등록된 안내가 없는 질환이에요. 식단은 수의사와 상의하세요."
- [x] Step 2: 실패 확인
- [x] Step 3: 구현
  - `FoodGuide`: `@Column(name="avoid_diseases", length=100) private String avoidDiseases;` (콤마 구분 키, 예 "OBESITY") + 생성자 인자 추가 + `List<String> avoidDiseaseKeys()`
  - `GuideDataLoader`: 고구마·바나나·단호박 → "OBESITY". **이미 데이터가 있는 DB 대비**: `run()` 에서 이름 → 키 Map 으로 `avoidDiseases` 가 null 인 행을 채우는 보정
  - `NutritionCalculator`: `DEFAULT_FOOD_KCAL_PER_KG = 3500`, `calc(weightKg, ageMonths, neutered, foodKcalPerKg, obese)`; obese 면 factor 1.0·stage "비만 감량 기준"; kcal 없으면 기본값으로 g 계산, `Result` 에 `boolean foodGramsEstimated`
  - `NutritionService`: SAFE 판정 순서 = 알레르기 → 지병(`dog.getDiseases()` 대문자 키 ∩ `avoidDiseaseKeys`) → safe. 지병 사유 `"%s이 있어 조금만 주세요"`(DiseaseGuide label). 가이드에 없는 지병 → `unknownDiseases` + notice
- [x] Step 4: `./gradlew test` 전체 통과(기존 테스트의 foodGrams null 기대가 있으면 사양대로 고침)
- [x] Step 5: 커밋 `feat: 지병 맞춤 영양 안내와 사료량 g 기본값`

### Task 3: 질문/기록 판별 규칙 IntentClassifier (항목 5 백엔드)
**Files:** Create `backend/.../record/service/IntentClassifier.java`, Test `backend/src/test/java/com/gaebalhagae/record/IntentClassifierTest.java`
**Produces:** `static boolean IntentClassifier.isQuestion(String text)`; 프론트 `frontend/src/utils/intent.ts` 의 `isQuestion` 이 같은 규칙(Task 8)

- [x] Step 1: 예문 30개(질문 15, 기록 15) `@ParameterizedTest @CsvSource`. 예: "포도 먹어도 돼요?"→true, "병원 가야 하나요"→true, "산책 시켜도 될까"→true, "이거 괜찮은가요"→true, "토했는데 어떡해"→true, "아침에 사료 50g 먹었어요"→false, "산책 30분 했다"→false, "설사함"→false
- [x] Step 2: 실패 확인
- [x] Step 3: 구현: 끝의 `[.!~ㅠㅜ\s]+` 제거 후 `?` 로 끝나거나 의문형 어미 정규식에 맞으면 true. "돼요/되요"는 앞에 "~도" 가 있을 때만(“먹어도 돼요”)
- [x] Step 4: 통과 수를 클래스 주석에 "정확도 n/30" 으로 기록
- [x] Step 5: 커밋 `feat: 질문·기록 판별 규칙과 예문 30개 테스트`

### Task 4: 내 기록을 아는 상담 (항목 8)
**Files:** Create `consult/service/RecentContextBuilder.java`; Modify `consult/domain/Consultation.java`(contextLines), `consult/service/ConsultService.java`, `consult/dto/ConsultResponses.java`(Detail 에 `List<String> contextLines`); Test `backend/src/test/java/com/gaebalhagae/consult/RecentContextBuilderTest.java`, `ConsultApiTest.java`
**Produces:** `List<String> RecentContextBuilder.build(Long dogId, LocalDate today)` (빈 리스트 = 기록 없음), `Detail.contextLines`

- [ ] Step 1: 실패 테스트
  - 기록 없음 → 빈 리스트
  - 식사 2·무른 변 1·증상 1(메모 "왼쪽 다리를 절어요")·산책 30분 → "식사 2회", "무른 변 1회", "증상 1회(마지막 M월 d일)", "산책·활동 하루 평균" 포함, **어느 줄에도 "다리를 절어요" 없음**
  - 체중 2건 4.0→4.4 → "체중 4 → 4.4kg" + ReportSignals 의 "10% 늘었어요" 문장
- [ ] Step 2: 실패 확인
- [ ] Step 3: 구현
  - 기간 `today.minusDays(13)..today`, `HealthRecordRepository.findByDogIdAndRecordDateBetweenOrderByRecordDateAscIdAsc`
  - 줄1 "식사 n회, 간식 n회, 배변 n회(무른 변 n회), 증상 n회(마지막 M월 d일)", 줄2 "투약·접종 n회, 병원 n회, 산책·활동 하루 평균 n분"(0 항목 생략), 줄3 체중, 그 뒤 `ReportSignals.of(...)`
  - 기록 0건이면 `List.of()`
  - `Consultation`: `@JdbcTypeCode(SqlTypes.JSON) @Column(name="context_lines") List<String> contextLines` + 생성자 인자
  - `ConsultService.prompt(dog, q, lines)`: lines 있으면 반려견 정보 다음에 "최근 14일 기록: …" + "기록은 참고용이고 판단은 질문 기준으로 하세요"
- [ ] Step 4: `./gradlew test` 통과
- [ ] Step 5: 커밋 `feat: 상담에 최근 14일 기록 집계를 함께 보냄`

### Task 5: 상담 원문 90일 정리 (항목 6)
**Files:** Create `consult/service/ConsultArchiveJob.java`; Modify `Consultation.java`(archivedAt + `archive(LocalDateTime)`), `ConsultationRepository.java`, `ConsultationAnswerRepository.java`, `ConsultResponses.java`(Detail·Item 에 `LocalDateTime archivedAt`), `application.yml`(`consult.retention-days: 90`); Test `backend/src/test/java/com/gaebalhagae/consult/ConsultArchiveJobTest.java`
**Produces:** `int ConsultArchiveJob.archiveOld()` (정리한 건수), `Detail.archivedAt`, `Item.archivedAt`

- [ ] Step 1: 실패 테스트 — 상담 3건 생성(ConsultApiTest 의 LLM 목 방식), JdbcTemplate 로 created_at 을 now-89일 / now-91일 / now-91일+archived_at 설정 → `archiveOld()` == 1, 91일 건 answers 0행·summary 그대로·archivedAt 있음, 89일 건 answers 그대로, 기록 행 수 그대로
- [ ] Step 2: 실패 확인
- [ ] Step 3: 구현
  - `ConsultationRepository.findArchiveTargets(cutoff, Pageable)`: `archivedAt is null and createdAt < cutoff order by id`
  - `ConsultationAnswerRepository.deleteByConsultationIdIn(List<Long>)` (@Modifying @Query)
  - Job: `@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")`, 100건씩 반복, 묶음마다 TransactionTemplate 로 answers 삭제 + `archive(now)`. `@Value("${consult.retention-days:90}")`
  - `ConsultService.detail/list` 에 archivedAt 전달
- [ ] Step 4: `./gradlew test` 통과
- [ ] Step 5: 커밋 `feat: 90일 지난 상담은 모델별 원문을 지우고 요약만 보관`

### Task 6: 프론트 — 타입·API + 코스 삭제 버튼 (항목 1)
**Files:** Modify `frontend/src/api/endpoints.ts`, `frontend/src/api/types.ts`, `frontend/src/pages/CoursesPage.tsx`, `frontend/src/pages/CoursePlanPage.tsx`
- [ ] Step 1: `courseApi.remove`; types 에 Task 2·4·5 새 필드(`foodGramsEstimated`, `weightLoss`, `unknownDiseases`, `FoodItem.reasonTag`, `contextLines`, `archivedAt`)
- [ ] Step 2: 목록 각 줄 오른쪽 휴지통, `e.preventDefault()`, 기존 확인 모달(ConsultPage 삭제와 같은 컴포넌트) "이 코스를 삭제할까요?"
- [ ] Step 3: CoursePlanPage 하단 "코스 삭제" → 확인 → remove → `navigate('/courses')`
- [ ] Step 4: `npm run lint && npm run build`
- [ ] Step 5: 커밋 `feat: 저장한 코스 목록·상세에서 코스 삭제`

### Task 7: 프론트 — 영양 화면 (항목 2·7)
**Files:** Modify `frontend/src/pages/NutritionPage.tsx` (홈 맞춤 안내가 kcal 를 쓰면 `HomePage.tsx` 도)
- [ ] Step 1: 맨 위 "{dog.name} 맞춤 안내" 박스: 지병 label + cautionText 첫 문장, unknownDiseases 문구
- [ ] Step 2: 지병 카드를 사료량 카드 위로
- [ ] Step 3: 사료량 카드: 큰 숫자 `약 {foodGrams}g`, weightLoss 면 "감량 기준 사료량", estimated 면 안내 문구 + 반려견 정보 링크, "하루 2번이면 한 번에 약 Ng", kcal 는 근거 줄, 간식 "(사료로 치면 약 Ng)"
- [ ] Step 4: 주의 음식에 reasonTag 배지
- [ ] Step 5: lint·build, 커밋 `feat: 영양 화면 맞춤 안내 박스와 g 표시`

### Task 8: 프론트 — 통합 입력창·바로 저장·어제 요약 (항목 3·4·5)
**Files:** Create `frontend/src/utils/intent.ts`, `frontend/src/components/record/UnifiedInput.tsx`; Modify `HomePage.tsx`, `RecordNewPage.tsx`, `RecordsPage.tsx`, `ConsultPage.tsx`
- [ ] Step 1: home-design 의 현재 HomePage·RecordNewPage 흐름을 읽고 이미 된 부분(바로 저장 등) 확인
- [ ] Step 2: `intent.ts isQuestion` = Task 3 규칙과 동일
- [ ] Step 3: `UnifiedInput`: "{dog.name} 보호자님! <내용>을 저장하도록 하겠습니다 !" → parse → batch (0개/실패면 `/records/new` 직접 입력으로 text 전달); 질문이면 병렬로 `consultApi.ask` → `/consult/{id}`, 아니면 `/records?date=` + `state: { savedIds, needsCheck }`
- [ ] Step 4: RecordsPage 안내 바 "기록 n개를 저장했어요 [되돌리기]"(5초), needsCheck 링크, 방금 저장한 항목 강조, "AI에게 물어보기" 링크
- [ ] Step 5: 홈 섹션 제목 "기록하고 물어보세요"; 요약을 어제 하루 + 칩 + "이번 달 기록 보기 →"
- [ ] Step 6: lint·build, 커밋 `feat: 기록·질문 통합 입력창과 어제 하루 요약`

### Task 9: 프론트 — 상담 화면 (항목 6·8)
**Files:** Modify `frontend/src/pages/ConsultPage.tsx`
- [ ] Step 1: archivedAt 있으면 목록·상세 "90일이 지나 요약만 보관 중" 배지, 모델별 탭 숨기고 종합 3줄만
- [ ] Step 2: contextLines 있으면 `<details>` "이번 답변에 사용한 내 기록"
- [ ] Step 3: lint·build, 커밋 `feat: 상담 보관 배지와 사용한 내 기록 상자`

### Task 10: 전체 확인
- [ ] `./gradlew test`, `npm run lint && npm run build` 모두 통과
- [ ] 사용자가 백엔드를 자기 터미널에서 띄운 뒤 브라우저로 홈·기록·상담·코스·영양 확인
- [ ] 사용자에게 push·PR 여부 묻기
