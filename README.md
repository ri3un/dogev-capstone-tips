# dogev-capstone-tips
개발하개🐶

LLM 기반 반려견 건강관리 및 맞춤형 외출 코스 추천 통합 플랫폼 (성결대 컴퓨터공학과 전공종합설계, 3번 팀 "개발하개")

- 프로젝트 전체 정리: [docs/졸업작품_통합정리.md](docs/졸업작품_통합정리.md)
- 주제 제안서: [TopicProposal.md](TopicProposal.md)

---

# 팀 규칙

2026-10-17에 정한 규칙입니다. 바꾸고 싶은 규칙은 목요일 회의에서 이야기하고, 정해지면 이 문서를 PR로 고칩니다.

## 1. 저장소 구조

저장소 하나에 백엔드와 프론트엔드를 폴더로 나눠서 둡니다.

```
dogev-capstone-tips/
├── backend/             # Spring Boot (Java 17, Gradle)
├── frontend/            # React + TypeScript (Vite)
├── docs/                # 기획·설계 문서
└── docker-compose.yml   # Nginx + Spring Boot + MySQL
```

## 2. 브랜치 규칙

| 브랜치 | 용도 |
| --- | --- |
| `main` | 발표·배포용. 직접 커밋하지 않음 |
| `develop` | 기능을 합치는 통합 브랜치 |
| `feature/기능명` | 기능 하나씩 작업. `develop`에서 만들고 `develop`으로 PR |

**작업 순서**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/login     # 기능 브랜치 만들기
# ... 작업, 커밋 ...
git push -u origin feature/login  # 푸시 후 GitHub에서 develop으로 PR 생성
```

- 브랜치 이름 예: `feature/login`, `feature/dog-profile`, `feature/place-map`
- 발표·배포 시점에 `develop` → `main`으로 합칩니다.

## 3. PR(Pull Request) 규칙

- `develop`에 합칠 때는 **반드시 PR**로 올리고 승인을 받은 뒤 합칩니다.
  - 백엔드: **이다인** 승인
  - 프론트엔드: **팀원 1명** 승인
- PR 하나에는 기능 하나만 담습니다.
- 관련 이슈가 있으면 PR 설명에 `#이슈번호`를 적습니다.

## 4. 커밋 메시지

**형식:** `타입: 한국어 설명`

| 타입 | 용도 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (README 등) |
| `refactor` | 코드 리팩토링 (기능 변경 없음) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드, 설정 파일 변경 등 |

**예시:**

```
feat: 카카오 로그인 기능 추가
fix: 반려견 체중 입력 검증 오류 수정
docs: README에 브랜치 규칙 추가
```

**규칙:**
- 제목은 50자 이내
- 명령문으로 작성 ("추가했다" ❌ → "추가" ⭕)
- 끝에 마침표 붙이지 않기
- 커밋은 작은 단위로, 기능 하나가 완성될 때마다

## 5. 공통 API 응답 형식

모든 API는 아래 형식으로 응답합니다.

```json
// 성공
{ "success": true, "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null, "error": { "code": "DOG_NOT_FOUND", "message": "반려견을 찾을 수 없습니다." } }
```

- HTTP 상태 코드: `400`, `401`, `403`, `404`, `500`
- LLM 3곳이 모두 실패하면 `503` + `LLM_UNAVAILABLE`
- 오류 코드는 백엔드의 `ErrorCode` enum 한 곳에서만 관리합니다.

## 6. 개발 환경

| 영역 | 버전·도구 |
| --- | --- |
| 백엔드 | Java 17, Spring Boot 3.5.x, Gradle, MySQL 8.4 LTS |
| 프론트엔드 | **Node 24 LTS** (`frontend/.nvmrc`에 고정), React + TypeScript, Vite, Tailwind CSS |
| 배포 | AWS EC2 1대, Docker Compose |

프론트엔드 작업 전에 `frontend/` 폴더에서 `nvm use`로 Node 버전을 맞춥니다.

## 7. DB 테이블 이름

- 소문자 **단수** snake_case (예: `health_record`, `place_condition`)
- 기본키는 `id`
- 모든 테이블에 `created_at`, `updated_at`

테이블 14개: `member`, `dog`, `health_record`, `vet_question`, `consultation`, `consultation_answer`, `place`, `place_condition`, `place_source`, `place_edit_request`, `course`, `course_place`, `food_guide`, `disease_guide`

## 8. 일정·작업 관리

| 도구 | 용도 |
| --- | --- |
| GitHub Issues | 기능·버그 단위 작업 |
| GitHub Projects | 칸반 보드로 일정 관리 |
| Notion | 회의록, 자료, 보고서 초안 |

## 9. 회의

- **매주 목요일 수업 시간**에 회의합니다.
- 회의록은 Notion에 남깁니다.

## 10. 시험 주 운영

- 시험 주에는 새 기능을 만들지 않고 **PR 리뷰, 버그 수정, 문서 작업**만 합니다.
- 대신 그 전 주에 기능 하나를 당겨서 끝냅니다.

## 11. 꼭 지킬 것

- **`main`, `develop`에 직접 작업하지 않기**: 항상 `feature/` 브랜치에서 작업 후 PR
- **push 전에 pull 먼저**: 충돌 방지
- **개인정보·비밀키 절대 커밋 금지**: API 키, 비밀번호 등은 `.env`에 넣고 `.gitignore`에 추가
- **`.gitignore` 설정**: `node_modules/`, `.env`, `.idea/`, `build/` 등 불필요한 파일 제외

---

# Git 기본 명령어

```bash
# 저장소 초기화
git init

# 파일 추가 (스테이징)
git add 파일명       # 특정 파일
git add .           # 전체 파일

# 커밋
git commit -m "커밋 메시지"

# 원격 저장소에 푸시
git push

# 원격 저장소에서 가져오기
git pull

# 상태 확인
git status

# 커밋 이력 확인
git log --oneline

# 브랜치 생성 및 이동
git branch 브랜치명
git checkout 브랜치명
git checkout -b 브랜치명   # 생성 + 이동 동시에

# 브랜치 병합 (합칠 대상 브랜치에서 실행)
git merge 브랜치명
```
