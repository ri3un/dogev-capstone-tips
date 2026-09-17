# dogev-capstone-tips
개발하개🐶

# Git 기본 명령어 & 컨벤션 정리

## 기본 명령어

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

# 브랜치 병합 (main에서 실행)
git merge 브랜치명
```

## 커밋 메시지 작성법

**형식:** `타입: 내용`

| 타입 | 용도 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (README 등) |
| `style` | 코드 포맷팅, 세미콜론 누락 등 (기능 변경 없음) |
| `refactor` | 코드 리팩토링 (기능 변경 없음) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드, 설정 파일 변경 등 |

**예시:**
feat: 로그인 기능 추가
fix: 회원가입 이메일 검증 오류 수정
docs: README에 설치 방법 추가


**규칙:**
- 제목은 50자 이내
- 명령문으로 작성 ("추가했다" ❌ → "추가" ⭕)
- 끝에 마침표 붙이지 않기

## 지켜야 할 것들

- **커밋은 작은 단위로** — 기능 하나 완성될 때마다 커밋
- **main 브랜치에 직접 작업하지 않기** — 기능별 브랜치 만들어서 작업 후 merge
- **.gitignore 설정** — `node_modules/`, `.env`, `.idea/` 등 불필요한 파일 제외
- **push 전에 pull 먼저** — 충돌 방지
- **개인정보·비밀키 절대 커밋 금지** — API 키, 비밀번호 등은 `.env`에 넣고 `.gitignore`에 추가
