# Spring Boot API 명세서

## 개요

LOL Highlight & Analysis 백엔드 API 문서입니다.

## Base URL

```
http://localhost:8080
```

## Authentication

대부분의 엔드포인트는 JWT 토큰 인증이 필요합니다.

**Header:**
```
Authorization: Bearer <JWT_TOKEN>
```

---

## 1. User API

### 1.1 현재 사용자 정보 조회

```http
GET /api/users/me
```

**Response**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "사용자",
  "profileImage": "https://example.com/profile.jpg",
  "riotId": "Hide on bush",
  "summonerName": "Hide on bush",
  "riotTagLine": "KR1",
  "provider": "GOOGLE",
  "role": "USER",
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:00"
}
```

### 1.2 사용자 정보 조회

```http
GET /api/users/{userId}
```

**Path Parameters:**
- `userId` (Long): 사용자 ID

**Response:** 1.1과 동일

### 1.3 사용자 정보 수정

```http
PUT /api/users/{userId}
```

**Request Body**
```json
{
  "name": "새이름",
  "profileImage": "https://example.com/new-profile.jpg"
}
```

**Response**
```json
{
  "id": 1,
  "name": "새이름",
  "profileImage": "https://example.com/new-profile.jpg",
  ...
}
```

### 1.4 라이엇 계정 연동

```http
POST /api/users/{userId}/riot-account
```

**Request Body**
```json
{
  "riotId": "Hide on bush#KR1",
  "summonerName": "Hide on bush",
  "tagLine": "KR1"
}
```

**Response**
```json
{
  "id": 1,
  "riotId": "Hide on bush#KR1",
  "summonerName": "Hide on bush",
  "riotTagLine": "KR1",
  ...
}
```

---

## 2. Match API

### 2.1 매치 목록 조회

```http
GET /api/matches?userId={userId}&page={page}&size={size}
```

**Query Parameters:**
- `userId` (Long, required): 사용자 ID
- `page` (Integer, optional): 페이지 번호 (default: 0)
- `size` (Integer, optional): 페이지 크기 (default: 20)

**Response**
```json
{
  "content": [
    {
      "id": 1,
      "matchId": "KR_1234567890",
      "championName": "Yasuo",
      "kills": 10,
      "deaths": 3,
      "assists": 8,
      "kda": 6.0,
      "win": true,
      "gameDuration": 1800,
      "gameCreation": 1704067200000,
      "status": "COMPLETED",
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 50,
  "totalPages": 3,
  "last": false,
  "first": true
}
```

**Match Status:**
- `PENDING`: 데이터 가져오는 중
- `COMPLETED`: 완료
- `FAILED`: 실패

### 2.2 매치 상세 조회

```http
GET /api/matches/{matchId}
```

**Path Parameters:**
- `matchId` (Long): 매치 ID

**Response**
```json
{
  "id": 1,
  "matchId": "KR_1234567890",
  "championName": "Yasuo",
  "kills": 10,
  "deaths": 3,
  "assists": 8,
  "kda": 6.0,
  "win": true,
  "gameDuration": 1800,
  "gameCreation": 1704067200000,
  "status": "COMPLETED",
  "timelineData": "...",
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:05"
}
```

### 2.3 매치 가져오기 (Import)

```http
POST /api/matches/import
```

**Request Body**
```json
{
  "matchId": "KR_1234567890"
}
```

**Response**
```json
{
  "id": 1,
  "matchId": "KR_1234567890",
  "championName": "Unknown",
  "status": "PENDING",
  "createdAt": "2025-01-01T00:00:00"
}
```

**처리 흐름:**
1. 즉시 PENDING 상태의 매치 생성 및 반환
2. 백그라운드에서 Riot API를 통해 매치 데이터 조회
3. 조회 완료 후 status를 COMPLETED로 변경
4. 클라이언트는 GET /api/matches/{matchId}로 폴링하여 완료 확인

### 2.4 사용자 매치 동기화

```http
POST /api/matches/sync
```

**Description:** 라이엇 계정 연동된 사용자의 최근 20개 매치를 자동으로 가져옵니다.

**Response**
```json
{
  "message": "Match sync initiated",
  "userId": 1
}
```

### 2.5 최근 매치 조회

```http
GET /api/matches/recent?userId={userId}&count={count}
```

**Query Parameters:**
- `userId` (Long, required): 사용자 ID
- `count` (Integer, optional): 가져올 매치 수 (default: 10, max: 20)

**Response**
```json
[
  {
    "id": 1,
    "matchId": "KR_1234567890",
    "championName": "Yasuo",
    "kills": 10,
    "deaths": 3,
    "assists": 8,
    "kda": 6.0,
    "win": true,
    ...
  }
]
```

### 2.6 매치 삭제

```http
DELETE /api/matches/{matchId}
```

**Response**
```json
{
  "message": "Match deleted successfully",
  "matchId": 1
}
```

---

## 3. Analysis API

### 3.1 분석 생성

```http
POST /api/analyses
```

**Request Body**
```json
{
  "matchId": 1
}
```

**Response**
```json
{
  "id": 1,
  "matchId": 1,
  "status": "PENDING",
  "createdAt": "2025-01-01T00:00:00"
}
```

**처리 흐름:**
1. 즉시 PENDING 상태의 분석 생성 및 반환
2. 백그라운드에서 Flask AI 서버로 분석 요청
3. AI 분석 완료 후 status를 COMPLETED로 변경
4. 클라이언트는 GET /api/analyses/{analysisId}로 폴링

### 3.2 분석 조회

```http
GET /api/analyses/{analysisId}
```

**Response**
```json
{
  "id": 1,
  "matchId": 1,
  "status": "COMPLETED",
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:05"
}
```

**Analysis Status:**
- `PENDING`: 분석 중
- `COMPLETED`: 완료
- `FAILED`: 실패

### 3.3 매치별 분석 조회

```http
GET /api/analyses/match/{matchId}
```

**Response:** 3.2와 동일

### 3.4 사용자 분석 목록 조회

```http
GET /api/analyses/user/{userId}?page={page}&size={size}
```

**Query Parameters:**
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**Response**
```json
{
  "content": [
    {
      "id": 1,
      "matchId": 1,
      "status": "COMPLETED",
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "pageable": {...},
  "totalElements": 10,
  "totalPages": 1
}
```

### 3.5 분석 삭제

```http
DELETE /api/analyses/{analysisId}
```

### 3.6 분석 재생성

```http
POST /api/analyses/{analysisId}/regenerate
```

**Description:** 기존 분석을 PENDING 상태로 변경하고 재분석을 시작합니다.

**Response**
```json
{
  "id": 1,
  "matchId": 1,
  "status": "PENDING",
  "updatedAt": "2025-01-01T00:10:00"
}
```

---

## 4. Highlight API

### 4.1 하이라이트 생성

```http
POST /api/highlights
```

**Request Body**
```json
{
  "matchId": 1,
  "title": "펜타킬 하이라이트",
  "description": "20분 팀파이트 펜타킬",
  "startTime": 1200,
  "endTime": 1230,
  "type": "PENTAKILL"
}
```

**Highlight Types:**
- `KILL`: 킬
- `MULTIKILL`: 멀티킬
- `PENTAKILL`: 펜타킬
- `BARON`: 바론
- `DRAGON`: 드래곤
- `TOWER`: 타워
- `TEAMFIGHT`: 팀파이트
- `CUSTOM`: 커스텀

**Response**
```json
{
  "id": 1,
  "matchId": 1,
  "title": "펜타킬 하이라이트",
  "description": "20분 팀파이트 펜타킬",
  "startTime": 1200,
  "endTime": 1230,
  "duration": 30,
  "type": "PENTAKILL",
  "status": "PENDING",
  "createdAt": "2025-01-01T00:00:00"
}
```

### 4.2 하이라이트 조회

```http
GET /api/highlights/{highlightId}
```

**Response**
```json
{
  "id": 1,
  "matchId": 1,
  "title": "펜타킬 하이라이트",
  "description": "20분 팀파이트 펜타킬",
  "startTime": 1200,
  "endTime": 1230,
  "duration": 30,
  "type": "PENTAKILL",
  "status": "COMPLETED",
  "videoUrl": "https://example.com/highlight.mp4",
  "thumbnailUrl": "https://example.com/thumbnail.jpg",
  "viewCount": 42,
  "createdAt": "2025-01-01T00:00:00"
}
```

**Highlight Status:**
- `PENDING`: 생성 중
- `PROCESSING`: 처리 중
- `COMPLETED`: 완료
- `FAILED`: 실패

### 4.3 매치별 하이라이트 목록

```http
GET /api/highlights/match/{matchId}?page={page}&size={size}
```

**Response**
```json
{
  "content": [
    {
      "id": 1,
      "matchId": 1,
      "title": "펜타킬 하이라이트",
      "type": "PENTAKILL",
      "status": "COMPLETED",
      "viewCount": 42,
      ...
    }
  ],
  "pageable": {...},
  "totalElements": 5
}
```

### 4.4 사용자 하이라이트 목록

```http
GET /api/highlights/user/{userId}?page={page}&size={size}
```

**Response:** 4.3과 동일 형식

### 4.5 자동 하이라이트 생성

```http
POST /api/highlights/auto/{matchId}
```

**Description:** AI가 매치를 분석하여 중요한 순간들을 자동으로 하이라이트로 생성합니다.

**Response**
```json
{
  "message": "Auto highlight generation initiated",
  "matchId": 1
}
```

**처리 흐름:**
1. Flask AI 서버로 매치 분석 요청
2. AI가 keyMoments 추출
3. 각 keyMoment를 하이라이트로 자동 생성
4. 클라이언트는 GET /api/highlights/match/{matchId}로 조회

### 4.6 하이라이트 삭제

```http
DELETE /api/highlights/{highlightId}
```

### 4.7 하이라이트 조회수 증가

```http
POST /api/highlights/{highlightId}/view
```

**Response**
```json
{
  "id": 1,
  "viewCount": 43,
  ...
}
```

---

## Error Responses

모든 에러는 다음 형식으로 반환됩니다:

```json
{
  "timestamp": "2025-01-01T00:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Match not found",
  "path": "/api/matches/999"
}
```

### HTTP Status Codes

- `200 OK`: 성공
- `201 Created`: 생성 성공
- `204 No Content`: 성공 (응답 본문 없음)
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 필요
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스 없음
- `409 Conflict`: 중복된 리소스
- `500 Internal Server Error`: 서버 오류

### Error Codes

**User Errors:**
- `USER_NOT_FOUND`: 사용자를 찾을 수 없음
- `INVALID_USER_DATA`: 잘못된 사용자 데이터

**Match Errors:**
- `MATCH_NOT_FOUND`: 매치를 찾을 수 없음
- `MATCH_ALREADY_EXISTS`: 이미 존재하는 매치
- `RIOT_API_ERROR`: Riot API 호출 실패

**Analysis Errors:**
- `ANALYSIS_NOT_FOUND`: 분석을 찾을 수 없음
- `ANALYSIS_ALREADY_EXISTS`: 이미 존재하는 분석
- `FLASK_API_ERROR`: Flask API 호출 실패

**Highlight Errors:**
- `HIGHLIGHT_NOT_FOUND`: 하이라이트를 찾을 수 없음
- `INVALID_TIME_RANGE`: 잘못된 시간 범위

---

## 비동기 처리 가이드

### 폴링 패턴

상태가 PENDING인 리소스는 폴링을 통해 완료를 확인합니다.

**예시: 매치 가져오기**

```javascript
// 1. 매치 가져오기 요청
const response = await fetch('/api/matches/import', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ matchId: 'KR_1234567890' })
});

const match = await response.json();
// { id: 1, status: 'PENDING' }

// 2. 폴링으로 완료 확인
const pollMatch = async (matchId) => {
  const interval = setInterval(async () => {
    const res = await fetch(`/api/matches/${matchId}`, {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    const data = await res.json();

    if (data.status === 'COMPLETED') {
      clearInterval(interval);
      console.log('매치 가져오기 완료:', data);
    } else if (data.status === 'FAILED') {
      clearInterval(interval);
      console.error('매치 가져오기 실패');
    }
  }, 2000); // 2초마다 확인
};

pollMatch(match.id);
```

**권장 폴링 간격:**
- 매치 가져오기: 2-3초
- AI 분석: 3-5초
- 하이라이트 생성: 2-3초

---

## Pagination

모든 목록 조회 API는 페이지네이션을 지원합니다.

**Query Parameters:**
- `page`: 페이지 번호 (0부터 시작)
- `size`: 페이지 크기
- `sort`: 정렬 기준 (예: `createdAt,desc`)

**Response 구조:**
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false }
  },
  "totalElements": 50,
  "totalPages": 3,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

---

## Rate Limiting

현재 Rate Limit은 없지만, 프로덕션 환경에서는 다음과 같이 적용 예정:

- 일반 사용자: 100 requests/minute
- 인증된 사용자: 1000 requests/minute

---

## 참고 사항

- 모든 날짜는 ISO 8601 형식 (`2025-01-01T00:00:00`)
- 모든 시간은 초 단위
- 모든 요청/응답은 UTF-8 인코딩
- Content-Type은 `application/json`
