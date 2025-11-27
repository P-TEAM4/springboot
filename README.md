# LOL Highlight Backend

AI 기반 LoL(League of Legends) 자동 하이라이트 생성 및 경기 분석 시스템의 백엔드 서버입니다.

## 프로젝트 개요

이 프로젝트는 LoL 경기 데이터를 분석하여 자동으로 하이라이트 영상을 생성하고, AI 기반 경기 분석 리포트를 제공하는 서비스의 백엔드 API 서버입니다.

### 주요 기능

- **사용자 인증**: Google OAuth2 기반 소셜 로그인 (향후 Riot 로그인 지원 예정)
- **Riot 계정 연동**: 소환사 계정 연동 및 매치 데이터 동기화
- **경기 데이터 관리**: Riot API를 통한 매치 데이터 수집 및 관리
- **자동 하이라이트 생성**: AI 기반 주요 장면 자동 추출 및 영상 생성
- **경기 분석 리포트**: ML 모델을 활용한 경기력 분석 및 개선 제안

## 기술 스택

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (개발), PostgreSQL (프로덕션)
- **Security**: Spring Security, OAuth2, JWT
- **ORM**: JPA/Hibernate
- **Build Tool**: Gradle
- **API Documentation**: Swagger/OpenAPI 3.0

## 시스템 아키텍처

### 전체 시스템 구조

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   Frontend      │      │   Spring Boot    │      │   AI Server     │
│   (React)       │◄────►│   Backend        │◄────►│   (FastAPI)     │
│                 │ HTTP │                  │ HTTP │                 │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                  │
                                  │ JDBC
                                  ▼
                         ┌──────────────────┐
                         │   PostgreSQL     │
                         │   Database       │
                         └──────────────────┘
                                  ▲
                                  │
                         ┌──────────────────┐
                         │   Riot Games     │
                         │   API            │
                         └──────────────────┘
```

### Spring Boot 백엔드 아키텍처

```
┌────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                    │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│  │ Controller   │   │ Controller   │   │ Controller   │       │
│  │ Layer        │   │ Layer        │   │ Layer        │       │
│  │ (User)       │   │ (Match)      │   │ (Highlight)  │       │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘       │
│         │                  │                  │                │
│  ┌──────▼───────┐   ┌──────▼───────┐   ┌──────▼───────┐       │
│  │ Service      │   │ Service      │   │ Service      │       │
│  │ Layer        │   │ Layer        │   │ Layer        │       │
│  │ (비즈니스    │   │ (경기 데이터)│   │ (하이라이트) │       │
│  │  로직)       │   │              │   │              │       │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘       │
│         │                  │                  │                │
│  ┌──────▼───────┐   ┌──────▼───────┐   ┌──────▼───────┐       │
│  │ Repository   │   │ Repository   │   │ Repository   │       │
│  │ Layer        │   │ Layer        │   │ Layer        │       │
│  │ (JPA)        │   │ (JPA)        │   │ (JPA)        │       │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘       │
│         │                  │                  │                │
│         └──────────────────┼──────────────────┘                │
│                            │                                   │
├────────────────────────────┼───────────────────────────────────┤
│         Database Layer     │                                   │
│                            ▼                                   │
│                  ┌──────────────────┐                          │
│                  │   PostgreSQL     │                          │
│                  │   H2 (Dev)       │                          │
│                  └──────────────────┘                          │
└────────────────────────────────────────────────────────────────┘
```

### 데이터 흐름

#### 1. 사용자 인증 및 경기 데이터 수집
```
User → Frontend → Spring Boot (OAuth2) → JWT Token
                      ↓
User → Frontend → Spring Boot (with JWT) → Riot API
                      ↓
                  Match Data Saved to DB
```

#### 2. 하이라이트 생성 플로우
```
User Request → Spring Boot → AI Server (FastAPI)
     (1)            (2)             (3)
                                     ↓
                              AI Analysis
                              (Event Detection)
                                     ↓
     (6)            (5)             (4)
Highlight Video ← Spring Boot ← AI Server
    Delivered       (Save)      (Video URL)
```

**단계별 설명**:
1. 사용자가 하이라이트 생성 요청
2. Spring Boot가 경기 데이터를 AI 서버로 전송
3. AI 서버가 경기 타임라인 분석 (Pentakill, Baron, Dragon 등)
4. 중요 이벤트 타임스탬프와 비디오 URL 반환
5. Spring Boot가 하이라이트 정보를 DB에 저장
6. 프론트엔드로 하이라이트 영상 전달

#### 3. 경기 분석 리포트 플로우
```
User Request → Spring Boot → AI Server (FastAPI)
     (1)            (2)             (3)
                                     ↓
                              Rule-based Model
                              (Gap Analysis)
                                     ↓
     (6)            (5)             (4)
Analysis Report ← Spring Boot ← AI Analysis Result
   Displayed        (Save)      (Scores + Insights)
```

**단계별 설명**:
1. 사용자가 경기 분석 요청
2. Spring Boot가 경기 통계를 AI 서버로 전송
3. AI 서버가 Rule-based 모델로 티어 베이스라인 대비 갭 분석
4. 5가지 점수 + 강점/약점/개선사항 반환
5. Spring Boot가 분석 리포트를 DB에 저장
6. 프론트엔드에 시각화된 리포트 표시

### 도메인 모델 관계도

```
┌─────────────┐
│    User     │
│ (사용자)    │
└──────┬──────┘
       │ 1:N
       │
       ▼
┌─────────────┐
│    Match    │
│  (경기)     │
└──────┬──────┘
       │ 1:N
       ├──────────────┐
       │              │
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│  Highlight  │  │  Analysis   │
│(하이라이트) │  │  (분석)     │
└─────────────┘  └─────────────┘
```

### 보안 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Security                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────┐       ┌────────────────┐           │
│  │ OAuth2 Login   │       │ JWT Auth       │           │
│  │ (Google)       │       │ Filter         │           │
│  └────────┬───────┘       └────────┬───────┘           │
│           │                        │                    │
│           ▼                        ▼                    │
│  ┌─────────────────────────────────────────┐           │
│  │  JWT Token Provider                     │           │
│  │  - Token Generation                     │           │
│  │  - Token Validation                     │           │
│  │  - User Authentication                  │           │
│  └─────────────────────────────────────────┘           │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### API 연동 구조

```
┌──────────────────┐
│  Spring Boot     │
│  Backend         │
└────────┬─────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌─────────┐ ┌─────────┐
│ Riot    │ │ AI      │
│ Games   │ │ Server  │
│ API     │ │ (Fast   │
│         │ │  API)   │
└─────────┘ └─────────┘
    │           │
    └─────┬─────┘
          │
     Match Data
     Analysis
```

**Riot API 사용**:
- Match-V5: 경기 데이터 조회
- Timeline-V5: 경기 타임라인 (이벤트) 조회
- Summoner-V4: 소환사 정보 조회
- League-V4: 랭크 티어 정보 조회

**AI Server API 사용**:
- POST `/api/v1/analyze/match`: 단일 경기 분석
- POST `/api/v1/analyze/gap`: Gap 분석 (티어 베이스라인 비교)
- POST `/api/v1/highlight/detect`: 하이라이트 이벤트 감지

## 프로젝트 구조

```
src/main/java/com/lol/highlight/
├── domain/                     # 도메인형 패키지 구조
│   ├── user/                  # 사용자 도메인
│   │   ├── entity/           # User, AuthProvider, UserRole
│   │   ├── repository/       # UserRepository
│   │   ├── service/          # UserService, CustomUserDetailsService, CustomOAuth2UserService
│   │   ├── controller/       # UserController
│   │   └── dto/              # UserResponse, UserUpdateRequest, etc.
│   ├── match/                # 경기 도메인
│   │   ├── entity/          # Match, MatchStatus
│   │   ├── repository/      # MatchRepository
│   │   ├── service/         # MatchService
│   │   ├── controller/      # MatchController
│   │   └── dto/             # MatchResponse, MatchImportRequest, etc.
│   ├── highlight/           # 하이라이트 도메인
│   │   ├── entity/         # Highlight, HighlightType, HighlightStatus
│   │   ├── repository/     # HighlightRepository
│   │   ├── service/        # HighlightService
│   │   ├── controller/     # HighlightController
│   │   └── dto/            # HighlightResponse, HighlightCreateRequest, etc.
│   └── analysis/           # 분석 도메인
│       ├── entity/        # Analysis, AnalysisStatus
│       ├── repository/    # AnalysisRepository
│       ├── service/       # AnalysisService
│       ├── controller/    # AnalysisController
│       └── dto/           # AnalysisResponse, AnalysisCreateRequest, etc.
└── global/                # 글로벌 설정 및 공통 기능
    ├── config/           # SecurityConfig, WebConfig
    ├── security/         # JwtTokenProvider, JwtAuthenticationFilter, OAuth2AuthenticationSuccessHandler
    ├── exception/        # ErrorCode, BusinessException, GlobalExceptionHandler
    └── common/           # BaseEntity
```

## 시작하기

### 사전 요구사항

- Java 17 이상
- Gradle 7.x 이상
- (선택) PostgreSQL 13 이상 (프로덕션 환경)

### 환경 변수 설정

이 프로젝트는 GitHub Secrets를 통해 환경 변수를 관리합니다.

#### 로컬 개발 환경

로컬 개발을 위해 `.env` 파일을 생성하세요 (`.env.example` 참고):

```bash
# .env.example 복사
cp .env.example .env

# .env 파일 수정
```

**필수 환경 변수**:
```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# JWT Secret (256-bit 이상)
JWT_SECRET=your-256-bit-secret-key-for-development-only

# Database (프로덕션)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=lol_highlight
DB_USERNAME=postgres
DB_PASSWORD=password
```

#### GitHub Secrets 설정 (프로덕션)

프로덕션 환경에서는 GitHub Repository Settings > Secrets and variables > Actions에서 다음 시크릿을 설정하세요:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `JWT_SECRET`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

**주의**: `.env` 파일은 절대 커밋하지 마세요. `.gitignore`에 등록되어 있습니다.

### 실행 방법

#### 개발 환경

```bash
# 의존성 설치 및 빌드
./gradlew build

# 개발 서버 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### 프로덕션 환경

```bash
# JAR 파일 빌드
./gradlew bootJar

# 실행
java -jar -Dspring.profiles.active=prod build/libs/lol-highlight-backend-0.0.1-SNAPSHOT.jar
```

서버가 정상적으로 실행되면 `http://localhost:8080`에서 접근할 수 있습니다.

### 개발 도구 접근

#### Swagger UI (API 문서)

Swagger UI를 통해 모든 API를 테스트하고 문서를 확인할 수 있습니다.

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

Swagger UI에서 JWT 인증이 필요한 API를 테스트하려면:
1. 우측 상단의 "Authorize" 버튼 클릭
2. JWT 토큰 입력 (Bearer 제외)
3. "Authorize" 클릭

#### H2 콘솔 (개발 환경)

개발 환경에서는 H2 인메모리 데이터베이스를 사용합니다.

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (공백)

---

## API 명세서

### 인증

모든 API는 `/oauth2/**`, `/login/**`, `/health` 를 제외하고 JWT 토큰 인증이 필요합니다.

**Authorization Header 형식:**
```
Authorization: Bearer {JWT_TOKEN}
```

### Base URL

```
http://localhost:8080/api
```

---

## 1. User API

### 1.1 현재 로그인한 사용자 정보 조회

```http
GET /api/users/me
```

**Response:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "profileImage": "https://example.com/profile.jpg",
  "riotId": "SummonerName#TAG",
  "summonerName": "SummonerName",
  "tagLine": "TAG",
  "provider": "GOOGLE",
  "role": "USER",
  "createdAt": "2024-01-01T00:00:00"
}
```

### 1.2 사용자 정보 조회

```http
GET /api/users/{id}
```

**Path Parameters:**
- `id` (Long): 사용자 ID

**Response:** 1.1과 동일

### 1.3 사용자 정보 수정

```http
PUT /api/users/{id}
```

**Request Body:**
```json
{
  "name": "Updated Name",
  "profileImage": "https://example.com/new-profile.jpg"
}
```

**Response:** 1.1과 동일

### 1.4 Riot 계정 연동

```http
POST /api/users/{id}/link-riot
```

**Request Body:**
```json
{
  "summonerName": "SummonerName",
  "tagLine": "TAG"
}
```

**Response:** 1.1과 동일

### 1.5 사용자 삭제

```http
DELETE /api/users/{id}
```

**Response:** `204 No Content`

---

## 2. Match API

### 2.1 경기 정보 조회

```http
GET /api/matches/{id}
```

**Response:**
```json
{
  "id": 1,
  "matchId": "KR_1234567890",
  "championName": "Ahri",
  "kills": 10,
  "deaths": 3,
  "assists": 15,
  "kda": 8.33,
  "win": true,
  "gameDuration": 1800,
  "gameCreation": 1640000000000,
  "status": "COMPLETED",
  "createdAt": "2024-01-01T00:00:00"
}
```

### 2.2 사용자의 경기 목록 조회 (페이징)

```http
GET /api/matches/user/{userId}?page=0&size=20&sort=gameCreation,desc
```

**Query Parameters:**
- `page` (int): 페이지 번호 (default: 0)
- `size` (int): 페이지 크기 (default: 20)
- `sort` (String): 정렬 기준

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "matchId": "KR_1234567890",
      "championName": "Ahri",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5
}
```

### 2.3 최근 경기 목록 조회

```http
GET /api/matches/user/{userId}/recent?count=20
```

**Query Parameters:**
- `count` (int): 조회할 경기 수 (default: 20)

**Response:**
```json
[
  {
    "id": 1,
    "matchId": "KR_1234567890",
    ...
  }
]
```

### 2.4 경기 가져오기

```http
POST /api/matches/user/{userId}/import
```

**Request Body:**
```json
{
  "matchId": "KR_1234567890"
}
```

**Response:** 2.1과 동일

### 2.5 경기 동기화

```http
POST /api/matches/user/{userId}/sync
```

Riot API에서 최근 경기 목록을 가져와 자동으로 저장합니다.

**Response:** `202 Accepted`

### 2.6 경기 삭제

```http
DELETE /api/matches/{id}
```

**Response:** `204 No Content`

---

## 3. Highlight API

### 3.1 하이라이트 정보 조회

```http
GET /api/highlights/{id}
```

**Response:**
```json
{
  "id": 1,
  "matchId": 1,
  "title": "Pentakill Highlight",
  "description": "Amazing pentakill moment",
  "videoUrl": "https://example.com/video.mp4",
  "thumbnailUrl": "https://example.com/thumbnail.jpg",
  "startTime": 1200,
  "endTime": 1230,
  "duration": 30,
  "type": "PENTAKILL",
  "status": "COMPLETED",
  "viewCount": 100,
  "createdAt": "2024-01-01T00:00:00"
}
```

### 3.2 경기의 하이라이트 목록 조회

```http
GET /api/highlights/match/{matchId}?page=0&size=20
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "matchId": 1,
      "title": "Pentakill Highlight",
      ...
    }
  ],
  "pageable": {...},
  "totalElements": 10,
  "totalPages": 1
}
```

### 3.3 사용자의 하이라이트 목록 조회

```http
GET /api/highlights/user/{userId}?page=0&size=20
```

**Response:** 3.2와 동일한 페이징 형식

### 3.4 하이라이트 생성

```http
POST /api/highlights
```

**Request Body:**
```json
{
  "matchId": 1,
  "title": "My Custom Highlight",
  "description": "Description here",
  "startTime": 1200,
  "endTime": 1230,
  "type": "CUSTOM"
}
```

**Response:** 3.1과 동일

### 3.5 자동 하이라이트 생성

```http
POST /api/highlights/match/{matchId}/auto-generate
```

AI가 자동으로 주요 장면을 추출하여 하이라이트를 생성합니다.

**Response:** `202 Accepted`

### 3.6 조회수 증가

```http
POST /api/highlights/{id}/view
```

**Response:** 3.1과 동일 (viewCount가 1 증가)

### 3.7 하이라이트 삭제

```http
DELETE /api/highlights/{id}
```

**Response:** `204 No Content`

---

## 4. Analysis API

### 4.1 분석 정보 조회

```http
GET /api/analyses/{id}
```

**Response:**
```json
{
  "id": 1,
  "matchId": 1,
  "strengthAnalysis": "강점: 팀파이트 기여도가 높고, 오브젝트 컨트롤이 우수합니다.",
  "weaknessAnalysis": "약점: 초반 라인전이 약하고, 시야 점수가 낮습니다.",
  "improvementSuggestions": "개선 제안: 와드 설치 빈도를 높이고, 초반 CS를 챙기는 연습이 필요합니다.",
  "scores": {
    "impactScore": 8.5,
    "teamFightScore": 9.0,
    "farmingScore": 6.5,
    "visionScore": 5.0,
    "objectiveControlScore": 8.0,
    "averageScore": 7.4
  },
  "status": "COMPLETED",
  "createdAt": "2024-01-01T00:00:00"
}
```

### 4.2 경기의 분석 조회

```http
GET /api/analyses/match/{matchId}
```

**Response:** 4.1과 동일

### 4.3 사용자의 분석 목록 조회

```http
GET /api/analyses/user/{userId}?page=0&size=20
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "matchId": 1,
      ...
    }
  ],
  "pageable": {...},
  "totalElements": 50,
  "totalPages": 3
}
```

### 4.4 분석 생성

```http
POST /api/analyses
```

**Request Body:**
```json
{
  "matchId": 1
}
```

**Response:** 4.1과 동일

### 4.5 분석 재생성

```http
POST /api/analyses/{id}/regenerate
```

기존 분석을 삭제하고 새로 분석을 시작합니다.

**Response:** `202 Accepted`

### 4.6 분석 삭제

```http
DELETE /api/analyses/{id}
```

**Response:** `204 No Content`

---

## 에러 응답 형식

모든 에러는 다음과 같은 형식으로 반환됩니다:

```json
{
  "code": "U001",
  "message": "User not found",
  "status": 404,
  "timestamp": "2024-01-01T00:00:00",
  "errors": [
    {
      "field": "email",
      "value": "invalid@email",
      "reason": "Invalid email format"
    }
  ]
}
```

### 주요 에러 코드

| 코드 | 메시지 | HTTP 상태 |
|------|--------|-----------|
| C001 | Internal server error | 500 |
| C002 | Invalid input value | 400 |
| C003 | Method not allowed | 405 |
| C004 | Entity not found | 404 |
| U001 | User not found | 404 |
| U002 | Email is duplicated | 409 |
| M001 | Match not found | 404 |
| M002 | Riot API error | 502 |
| H001 | Highlight not found | 404 |
| H002 | Highlight generation failed | 500 |
| A001 | Analysis not found | 404 |
| AUTH001 | Unauthorized | 401 |
| AUTH002 | Invalid token | 401 |
| AUTH003 | Expired token | 401 |

---

## OAuth2 로그인 플로우

### Google OAuth2

1. 프론트엔드에서 사용자를 `/oauth2/authorization/google`로 리다이렉트
2. Google 로그인 완료 후 `/login/oauth2/code/google`로 콜백
3. 백엔드에서 JWT 토큰 생성 후 프론트엔드 Redirect URI로 리다이렉트
   - 예: `http://localhost:3000/auth/callback?token={JWT_TOKEN}`
4. 프론트엔드에서 토큰을 저장하고 이후 요청에 사용

---

## 개발 가이드

### 새로운 도메인 추가 시

1. `domain/{domain_name}` 패키지 생성
2. 하위 패키지 생성: `entity`, `repository`, `service`, `controller`, `dto`
3. Entity는 `BaseEntity`를 상속받아 생성
4. Repository는 `JpaRepository` 인터페이스 상속
5. Service에서 비즈니스 로직 구현
6. Controller에서 REST API 엔드포인트 정의
7. 필요한 경우 커스텀 예외를 `ErrorCode`에 추가

### 에러 처리

- 비즈니스 로직 에러: `BusinessException` 사용
- 새로운 에러 코드는 `ErrorCode` enum에 추가
- `GlobalExceptionHandler`가 자동으로 에러 응답 변환

---

## 향후 개발 계획

- [ ] Riot OAuth2 로그인 통합
- [ ] Riot API 실제 연동
- [ ] AI 서버와의 비동기 통신 구현
- [ ] 하이라이트 영상 생성 기능 구현
- [ ] 경기 분석 ML 모델 통합
- [ ] WebSocket을 통한 실시간 처리 상태 알림
- [ ] Redis 캐싱 적용
- [ ] 배포 환경 구성 (Docker, CI/CD)

---

## 라이선스

이 프로젝트는 교육 목적으로 작성되었습니다.

## 팀원

- 김문기, 송재곤, 김예진, 김혁진, 백민석
