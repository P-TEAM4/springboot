# API 플로우 문서

## 개요
이 문서는 LoL Highlight 프로젝트의 모든 API 엔드포인트와 그 구현 상태를 정리한 것입니다.

---

## 1. API 구현 현황 요약

| 기능 | HTTP Method | URL | 구현 상태 | 비고 |
|------|-------------|-----|----------|------|
| 소환사 전적 조회 | GET | /api/matches/summoner/{gameName}/{tagLine} | 완료 | |
| 전적 갱신 | POST | /api/matches/summoner/{gameName}/{tagLine}/refresh | 완료 | |
| 매치 상세정보 조회 | GET | /api/matches/{matchId}/detail | 완료 | |
| 사용자 정보 수정 | PUT | /api/users | 완료 | @AuthUser로 본인 정보 수정 |
| 사용자 삭제 | DELETE | /api/users | 완료 | @AuthUser로 본인 계정 삭제 |
| Google OAuth 로그인 | POST | /api/auth/google | 완료 | |
| 토큰 갱신 | POST | /api/auth/refresh | 완료 | |
| 로그아웃 | POST | /api/auth/logout | 완료 | |
| 분석정보 조회 | GET | /api/analyses/{id} | 완료 | |
| 매치의 분석 조회 | GET | /api/analyses/match/{matchId} | 완료 | FastAPI 연동 TODO |
| 플레이어의 분석 목록 조회 | GET | /api/analyses/player/{puuid} | 완료 | |
| 분석 삭제 | DELETE | /api/analyses/{id} | 완료 | |
| 하이라이트 정보 조회 | GET | /api/highlights/{id} | 완료 | |
| 플레이어의 하이라이트 목록 조회 | GET | /api/highlights/player/{puuid} | 완료 | |
| AI 자동 하이라이트 생성 | POST | /api/highlights/match/{matchId}/auto-generate | 완료 | FastAPI 연동 TODO |
| 하이라이트 조회수 증가 | POST | /api/highlights/{id}/view | 완료 | |
| 하이라이트 삭제 | DELETE | /api/highlights/{id} | 완료 | |

---

## 2. Auth API (인증)

### 2.1 Google OAuth 로그인
```
POST /api/auth/google
```

**플로우:**
1. 클라이언트(Electron)에서 Google ID Token을 받아서 전송
2. AuthService에서 Google tokeninfo API로 토큰 검증
3. 이메일로 기존 사용자 조회, 없으면 신규 생성
4. JWT Access Token과 Refresh Token 생성
5. 응답 헤더에 토큰 설정 (Access-Token, Refresh-Token)

**Request Body:**
```json
{
  "idToken": "Google ID Token"
}
```

**Response Headers:**
```
Access-Token: {JWT Access Token}
Refresh-Token: {JWT Refresh Token}
```

---

### 2.2 토큰 갱신
```
POST /api/auth/refresh
```

**플로우:**
1. Refresh-Token 헤더에서 토큰 추출
2. 토큰 유효성 검증
3. 블랙리스트 확인
4. 새로운 Access/Refresh Token 생성
5. 기존 Refresh Token 블랙리스트에 추가
6. 응답 헤더에 새 토큰 설정

**Request Headers:**
```
Refresh-Token: {Refresh Token}
```

---

### 2.3 로그아웃
```
POST /api/auth/logout
```

**플로우:**
1. Authorization 헤더에서 Access Token 추출
2. 토큰을 블랙리스트에 추가

**Request Headers:**
```
Authorization: Bearer {Access Token}
```

---

## 3. User API (사용자)

### 3.1 현재 로그인한 사용자 정보 조회
```
GET /api/users/me
```

**인증:** 필수 (@AuthUser)

**플로우:**
1. @AuthUser 어노테이션으로 JWT 토큰에서 User 엔티티 직접 주입
2. UserResponse DTO로 변환하여 반환

---

### 3.2 사용자 정보 조회 (타인)
```
GET /api/users/{id}
```

**인증:** 불필요 (비로그인 사용자도 조회 가능)

**플로우:**
1. Path Variable에서 사용자 ID 추출
2. UserService에서 사용자 정보 조회
3. 없으면 USER_NOT_FOUND 예외 발생

---

### 3.3 사용자 정보 수정
```
PUT /api/users
```

**인증:** 필수 (@AuthUser)

**플로우:**
1. @AuthUser 어노테이션으로 현재 로그인한 사용자 정보 주입
2. Request Body에서 수정할 정보 추출 (name, profileImage)
3. User 엔티티의 updateProfile() 메소드로 정보 수정

**Request Body:**
```json
{
  "name": "새로운 이름",
  "profileImage": "새로운 프로필 이미지 URL"
}
```

---

### 3.4 Riot 계정 연동
```
POST /api/users/link-riot
```

**인증:** 필수 (@AuthUser)

**플로우:**
1. @AuthUser 어노테이션으로 현재 로그인한 사용자 정보 주입
2. Request Body에서 소환사명, 태그라인 추출
3. Riot ID 형식으로 변환 (summonerName#tagLine)
4. 이미 연동된 계정인지 확인
5. User 엔티티에 Riot 계정 정보 연동

**TODO:** Riot API를 통한 실제 계정 검증 필요

**Request Body:**
```json
{
  "summonerName": "소환사명",
  "tagLine": "KR1"
}
```

---

### 3.5 사용자 삭제
```
DELETE /api/users
```

**인증:** 필수 (@AuthUser)

**플로우:**
1. @AuthUser 어노테이션으로 현재 로그인한 사용자 정보 주입
2. 사용자 삭제

---

## 4. Match API (매치/전적)

### 4.1 소환사 전적 조회
```
GET /api/matches/summoner/{gameName}/{tagLine}
```

**플로우:**
1. 요청한 사용자(JWT 토큰) 확인
2. Riot API로 소환사 PUUID 조회
3. DB에 매치 정보 존재 여부 확인
4. Rate Limit 체크
5. 필요시 Riot API에서 새로운 매치 정보 가져와서 저장
6. 오래된 매치 정리 (최근 N개만 유지)
7. 사용자 활동 시간 업데이트
8. DB에서 페이징하여 매치 목록 반환

**Rate Limit:**
- 설정된 시간 윈도우 내 요청 횟수 제한
- 제한 초과 시 캐시된 데이터 반환

**Query Parameters:**
- page: 페이지 번호 (0부터 시작)
- size: 페이지 크기
- sort: 정렬 기준

---

### 4.2 전적 강제 갱신
```
POST /api/matches/summoner/{gameName}/{tagLine}/refresh
```

**플로우:**
1. 요청한 사용자(JWT 토큰) 확인
2. Rate Limit 체크 (초과 시 예외 발생)
3. Riot API로 소환사 PUUID 조회
4. Riot API에서 최신 매치 정보 가져와서 저장
5. 오래된 매치 정리
6. Rate Limit 기록 업데이트

---

### 4.3 매치 상세 정보 조회
```
GET /api/matches/{matchId}/detail
```

**주의:** matchId는 Riot Match ID 형식 (예: KR_7951942780)

**플로우:**
1. matchId로 DB에서 Match 엔티티 조회
2. Match의 detailDataUrl로 Cloud Storage에서 상세 데이터 다운로드
3. 플레이어별 아이템 정보를 Data Dragon API에서 조회
4. MatchDetailResponse DTO로 변환하여 반환

**Response:**
- 매치 ID, 게임 버전
- 플레이어 상세 정보 (챔피언, KDA, 아이템, CS 등)
- 팀 정보 (승패, 오브젝트 처치 등)

---

## 5. Analysis API (분석)

### API 겹침 여부 참고
- **5.1 GET /api/analyses/{id}**: 분석 테이블의 PK(ID)로 조회 - 이미 분석 ID를 알고 있을 때 사용
- **5.2 GET /api/analyses/match/{matchId}**: 매치 테이블의 FK(matchId)로 조회 - 매치 ID만 알고 있을 때 사용
- 두 API는 조회 기준이 다르므로 겹치지 않음

### 5.1 분석 정보 조회 (분석 ID로)
```
GET /api/analyses/{id}
```

**플로우:**
1. Path Variable에서 분석 ID 추출
2. AnalysisRepository에서 분석 정보 조회
3. 없으면 ANALYSIS_NOT_FOUND 예외 발생

---

### 5.2 매치의 분석 조회 (매치 ID로)
```
GET /api/analyses/match/{matchId}
```

**주의:** matchId는 내부 DB ID (Long 타입)

**플로우:**
1. matchId로 분석 정보 조회
2. 없으면 ANALYSIS_NOT_FOUND 예외 발생

**TODO: [FastAPI 연동]**
DB에 분석 정보가 없을 경우:
1. PENDING 상태의 Analysis 엔티티 생성
2. FastAPI 서버에 비동기 분석 요청 (/api/v1/analyze/gap-analysis)
3. FastAPI 콜백 또는 폴링으로 결과 수신 후 상태 업데이트

---

### 5.3 플레이어의 분석 목록 조회
```
GET /api/analyses/player/{puuid}
```

**플로우:**
1. puuid로 해당 플레이어의 모든 분석 조회
2. 페이징 처리하여 반환

**Query Parameters:**
- page, size, sort

---

### 5.4 경기 분석 생성
```
POST /api/analyses
```

**플로우:**
1. matchId로 Match 엔티티 조회
2. 이미 분석이 존재하는지 확인
3. PENDING 상태로 Analysis 엔티티 생성
4. AI 서버에 비동기로 분석 요청 (AiAnalysisClient)

**Request Body:**
```json
{
  "matchId": 1
}
```

---

### 5.5 분석 삭제
```
DELETE /api/analyses/{id}
```

**플로우:**
1. 분석 존재 여부 확인
2. 분석 삭제

---

## 6. Highlight API (하이라이트)

### API 겹침 여부 참고
- **6.5 POST /api/highlights/match/{matchId}/auto-generate**: AI 자동 하이라이트 생성 - 매치 데이터를 AI가 분석하여 주요 장면 추출
- **6.6 POST /api/highlights/{id}/view**: 하이라이트 조회수 증가 - 기존 하이라이트의 조회수를 1 증가
- 두 API는 완전히 다른 기능이므로 겹치지 않음

### 6.1 하이라이트 정보 조회
```
GET /api/highlights/{id}
```

**플로우:**
1. Path Variable에서 하이라이트 ID 추출
2. HighlightRepository에서 조회
3. 없으면 HIGHLIGHT_NOT_FOUND 예외 발생

---

### 6.2 매치의 하이라이트 목록 조회
```
GET /api/highlights/match/{matchId}
```

**플로우:**
1. matchId로 해당 매치의 모든 하이라이트 조회
2. 페이징 처리하여 반환

---

### 6.3 플레이어의 하이라이트 목록 조회
```
GET /api/highlights/player/{puuid}
```

**플로우:**
1. puuid로 해당 플레이어의 모든 하이라이트 조회
2. 페이징 처리하여 반환

---

### 6.4 하이라이트 생성
```
POST /api/highlights
```

**플로우:**
1. matchId로 Match 엔티티 조회
2. duration 계산 (endTime - startTime)
3. PENDING 상태로 Highlight 엔티티 생성

**TODO: [FastAPI 연동]**
PENDING 상태로 저장 후:
1. FastAPI 서버에 비동기 영상 생성 요청
2. FastAPI 콜백으로 영상 URL 수신 후 상태 업데이트

**Request Body:**
```json
{
  "matchId": 1,
  "title": "하이라이트 제목",
  "description": "설명",
  "startTime": 120,
  "endTime": 150,
  "type": "KILL"
}
```

---

### 6.5 AI 자동 하이라이트 생성
```
POST /api/highlights/match/{matchId}/auto-generate
```

**플로우:**
1. matchId로 Match 엔티티 조회

**TODO: [FastAPI 연동]**
1. FastAPI 서버에 매치 데이터 전송 (/api/v1/highlight/auto-generate)
2. AI가 킬/타워/오브젝트 등 주요 장면 자동 추출
3. 추출된 장면별로 Highlight 엔티티 생성 (PENDING 상태)
4. FastAPI 콜백으로 영상 URL 수신 후 상태 업데이트

---

### 6.6 하이라이트 조회수 증가
```
POST /api/highlights/{id}/view
```

**플로우:**
1. 하이라이트 조회
2. viewCount 1 증가
3. 수정된 하이라이트 정보 반환

---

### 6.7 하이라이트 삭제
```
DELETE /api/highlights/{id}
```

**플로우:**
1. 하이라이트 존재 여부 확인
2. 하이라이트 삭제

---

## 7. FastAPI 연동 설정

### 7.1 설정 파일 (application.yml)
```yaml
ai:
  server:
    base-url: ${AI_SERVER_URL:http://localhost:8000}  # TODO: 실제 서버 URL로 변경
    timeout: 30000           # 읽기 타임아웃 (ms)
    connect-timeout: 5000    # 연결 타임아웃 (ms)
    max-retries: 3           # 재시도 횟수
```

### 7.2 FastAPI 엔드포인트 (임시)
| 엔드포인트 | 설명 | 요청 데이터 |
|-----------|------|-----------|
| POST /api/v1/analyze/gap | 갭 분석 | matchId, puuid, tier |
| POST /api/v1/analyze/match | 매치 분석 | match_id, summoner_name, tag_line |
| POST /api/v1/highlight/generate | 하이라이트 영상 생성 | matchId, startTime, endTime, highlightId |
| POST /api/v1/highlight/auto-generate | AI 자동 하이라이트 추출 | matchId, puuid |

### 7.3 에러 처리
AI 서버 연결 실패 시 다음 에러 코드가 반환됩니다:

| 에러 코드 | HTTP 상태 | 설명 |
|----------|----------|------|
| AI001 | 503 Service Unavailable | AI 서버 연결 실패 |
| AI002 | 504 Gateway Timeout | AI 서버 응답 시간 초과 |
| AI003 | 502 Bad Gateway | AI 서버 오류 응답 |
| AI004 | 202 Accepted | AI 분석 진행 중 |

### 7.4 AI 클라이언트 파일 위치
| 클라이언트 | 파일 경로 |
|-----------|----------|
| AiAnalysisClient | src/main/java/com/lol/highlight/domain/analysis/service/AiAnalysisClient.java |
| AiHighlightClient | src/main/java/com/lol/highlight/domain/highlight/service/AiHighlightClient.java |
| AiClientConfig | src/main/java/com/lol/highlight/global/config/AiClientConfig.java |

---

## 8. TODO 항목

### 8.1 FastAPI 서버 배포 후
| 항목 | 내용 |
|------|------|
| AI_SERVER_URL 환경변수 설정 | 실제 FastAPI 서버 URL로 변경 |
| 엔드포인트 URL 확인 | FastAPI 서버의 실제 엔드포인트에 맞게 수정 |
| 요청/응답 DTO 수정 | FastAPI 서버의 실제 형식에 맞게 수정 |

### 8.2 AnalysisService.getAnalysisByMatchId()
DB에 분석 정보가 없을 경우 자동으로 FastAPI에 요청하는 로직 추가 필요:
1. Match 존재 여부 확인
2. PENDING 상태로 Analysis 생성
3. FastAPI에 비동기 분석 요청
4. PENDING 상태의 AnalysisResponse 반환

### 8.3 User 관련
| 위치 | 내용 |
|------|------|
| UserService.linkRiotAccount() | Riot API를 통한 실제 계정 검증 |

---

## 9. 추가 구현된 API (스크린샷에 없음)

| HTTP Method | URL | 설명 |
|-------------|-----|------|
| GET | /api/users/me | 현재 로그인한 사용자 정보 조회 |
| GET | /api/users/{id} | 특정 사용자 정보 조회 (비로그인 가능) |
| POST | /api/users/link-riot | Riot 계정 연동 |
| POST | /api/analyses | 경기 분석 생성 |
| POST | /api/highlights | 하이라이트 생성 |
| GET | /api/highlights/match/{matchId} | 매치의 하이라이트 목록 조회 |
| POST | /api/auth/test-token | [DEV] 테스트용 JWT 토큰 발급 |
| GET | / | 루트 헬스 체크 |
| GET | /health | 서비스 상태 확인 |

---

## 10. 파일 위치

| 컨트롤러 | 파일 경로 |
|---------|----------|
| AuthController | src/main/java/com/lol/highlight/global/auth/controller/AuthController.java |
| UserController | src/main/java/com/lol/highlight/domain/user/controller/UserController.java |
| MatchController | src/main/java/com/lol/highlight/domain/match/controller/MatchController.java |
| AnalysisController | src/main/java/com/lol/highlight/domain/analysis/controller/AnalysisController.java |
| HighlightController | src/main/java/com/lol/highlight/domain/highlight/controller/HighlightController.java |

| 서비스 | 파일 경로 |
|--------|----------|
| AuthService | src/main/java/com/lol/highlight/global/auth/service/AuthService.java |
| UserService | src/main/java/com/lol/highlight/domain/user/service/UserService.java |
| MatchService | src/main/java/com/lol/highlight/domain/match/service/MatchService.java |
| AnalysisService | src/main/java/com/lol/highlight/domain/analysis/service/AnalysisService.java |
| HighlightService | src/main/java/com/lol/highlight/domain/highlight/service/HighlightService.java |

| 인증 관련 | 파일 경로 |
|----------|----------|
| @AuthUser 어노테이션 | src/main/java/com/lol/highlight/global/auth/annotation/AuthUser.java |
| AuthUserArgumentResolver | src/main/java/com/lol/highlight/global/auth/resolver/AuthUserArgumentResolver.java |
