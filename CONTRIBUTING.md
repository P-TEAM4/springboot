# Contributing Guide

## Git Branch 전략

이 프로젝트는 Git Flow 브랜치 전략을 사용합니다.

### 브랜치 구조

```
main          - 프로덕션 배포 브랜치 (항상 안정적인 상태 유지)
  └─ develop  - 개발 통합 브랜치 (다음 릴리즈를 위한 개발)
       ├─ feature/*   - 새로운 기능 개발
       ├─ bugfix/*    - 버그 수정
       └─ hotfix/*    - 긴급 수정
```

### 브랜치 규칙

#### 1. main 브랜치
- **목적**: 프로덕션 배포용
- **보호**: 직접 커밋 금지, Pull Request를 통해서만 병합
- **배포**: main 브랜치에 병합되면 자동 배포

#### 2. develop 브랜치
- **목적**: 다음 릴리즈를 위한 개발 통합
- **병합**: feature, bugfix 브랜치가 여기로 병합됨
- **보호**: 직접 커밋 지양, Pull Request 권장

#### 3. feature 브랜치
- **명명**: `feature/기능명` (예: `feature/user-authentication`)
- **시작점**: develop 브랜치에서 분기
- **병합**: develop 브랜치로 Pull Request
- **삭제**: 병합 후 삭제

#### 4. bugfix 브랜치
- **명명**: `bugfix/버그명` (예: `bugfix/login-error`)
- **시작점**: develop 브랜치에서 분기
- **병합**: develop 브랜치로 Pull Request
- **삭제**: 병합 후 삭제

#### 5. hotfix 브랜치
- **명명**: `hotfix/버그명` (예: `hotfix/critical-security-fix`)
- **시작점**: main 브랜치에서 분기
- **병합**: main과 develop 브랜치 모두에 병합
- **삭제**: 병합 후 삭제

## 작업 플로우

### 새로운 기능 개발

```bash
# 1. develop 브랜치 최신화
git checkout develop
git pull origin develop

# 2. feature 브랜치 생성
git checkout -b feature/new-feature

# 3. 개발 작업 수행
# ... 코드 작성 ...

# 4. 커밋
git add .
git commit -m "feat: Add new feature"

# 5. 원격 저장소에 푸시
git push origin feature/new-feature

# 6. GitHub에서 Pull Request 생성 (feature/new-feature -> develop)
```

### 버그 수정

```bash
# 1. develop 브랜치에서 bugfix 브랜치 생성
git checkout develop
git pull origin develop
git checkout -b bugfix/fix-bug

# 2. 버그 수정
# ... 코드 수정 ...

# 3. 커밋 및 푸시
git add .
git commit -m "fix: Fix bug description"
git push origin bugfix/fix-bug

# 4. Pull Request 생성 (bugfix/fix-bug -> develop)
```

### 긴급 수정 (Hotfix)

```bash
# 1. main 브랜치에서 hotfix 브랜치 생성
git checkout main
git pull origin main
git checkout -b hotfix/critical-fix

# 2. 긴급 수정
# ... 코드 수정 ...

# 3. 커밋 및 푸시
git add .
git commit -m "hotfix: Critical security fix"
git push origin hotfix/critical-fix

# 4. Pull Request 생성
#    - hotfix/critical-fix -> main
#    - hotfix/critical-fix -> develop
```

## 커밋 메시지 규칙

Conventional Commits 형식을 따릅니다.

### 형식

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅, 세미콜론 누락 등 (기능 변경 없음)
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드 추가/수정
- `chore`: 빌드 업무, 패키지 매니저 설정 등

### 예시

```bash
# 좋은 예시
git commit -m "feat(user): Add Google OAuth2 login"
git commit -m "fix(match): Fix match import error"
git commit -m "docs: Update API documentation"

# 나쁜 예시
git commit -m "update"
git commit -m "fix bug"
git commit -m "asdf"
```

## Pull Request 규칙

### PR 제목

- 커밋 메시지와 동일한 형식 사용
- 예: `feat(highlight): Add auto highlight generation`

### PR 설명

```markdown
## 변경 사항
- 변경된 내용을 간단히 설명

## 테스트
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] 수동 테스트 완료

## 체크리스트
- [ ] 코드 리뷰 요청
- [ ] 문서 업데이트
- [ ] 관련 이슈 연결
```

### 코드 리뷰

- 최소 1명 이상의 승인 필요
- 모든 대화 해결 후 병합
- CI/CD 테스트 통과 필수

## 코드 스타일

- Java 코드 스타일: Google Java Style Guide
- Lombok 적극 활용
- REST API 네이밍: RESTful 규칙 준수
- 변수명: camelCase
- 클래스명: PascalCase
- 상수: UPPER_SNAKE_CASE

## 기타 규칙

1. **테스트 작성**: 새로운 기능에는 반드시 테스트 코드 작성
2. **문서화**: API 변경 시 README 업데이트
3. **이슈 연결**: PR에 관련 이슈 번호 명시
4. **브랜치 삭제**: 병합 후 feature/bugfix 브랜치는 즉시 삭제
5. **충돌 해결**: 병합 전 develop 브랜치 최신 상태로 rebase

## 문의

질문이나 제안사항이 있으면 이슈를 생성해주세요.
