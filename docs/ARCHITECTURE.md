# Spring Boot 아키텍처 문서

## 프로젝트 구조

```
springboot/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/lol/highlight/
│   │   │       ├── LolHighlightApplication.java
│   │   │       ├── domain/
│   │   │       │   ├── analysis/
│   │   │       │   │   ├── controller/
│   │   │       │   │   ├── dto/
│   │   │       │   │   ├── entity/
│   │   │       │   │   ├── repository/
│   │   │       │   │   └── service/
│   │   │       │   ├── highlight/
│   │   │       │   ├── match/
│   │   │       │   └── user/
│   │   │       └── global/
│   │   │           ├── client/           # 외부 API 클라이언트
│   │   │           │   ├── dto/
│   │   │           │   ├── FlaskApiClient.java
│   │   │           │   └── RiotApiClient.java
│   │   │           ├── common/
│   │   │           ├── config/           # 설정
│   │   │           │   ├── AsyncConfig.java
│   │   │           │   ├── RestTemplateConfig.java
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   ├── SwaggerConfig.java
│   │   │           │   └── WebConfig.java
│   │   │           ├── exception/
│   │   │           ├── security/
│   │   │           └── service/          # 통합 서비스
│   │   │               ├── FlaskIntegrationService.java
│   │   │               └── RiotApiService.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
└── docs/
    ├── SPRING_BOOT_API.md
    └── ARCHITECTURE.md
```

## 레이어 아키텍처

```
┌─────────────────────────────────────────┐
│          Presentation Layer             │
│         (Controller, DTO)               │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│          Service Layer                  │
│    (Business Logic, Integration)        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│       Repository Layer                  │
│          (Data Access)                  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│          Database                       │
└─────────────────────────────────────────┘
```

### 1. Presentation Layer (Controller)

**역할:** HTTP 요청/응답 처리

**주요 클래스:**
- `UserController`
- `MatchController`
- `AnalysisController`
- `HighlightController`

**예시:**
```java
@RestController
@RequestMapping("/api/matches")
public class MatchController {
    @PostMapping("/import")
    public ResponseEntity<MatchResponse> importMatch(
        @RequestBody MatchImportRequest request
    ) {
        // Service 계층 호출
        MatchResponse response = matchService.importMatch(userId, request);
        return ResponseEntity.ok(response);
    }
}
```

### 2. Service Layer

**역할:** 비즈니스 로직 처리 및 외부 서비스 통합

**Domain Services:**
- `UserService`: 사용자 관리
- `MatchService`: 매치 관리
- `AnalysisService`: 분석 관리
- `HighlightService`: 하이라이트 관리

**Integration Services:**
- `FlaskIntegrationService`: Flask AI 서버 연동
- `RiotApiService`: Riot API 연동

**예시:**
```java
@Service
public class MatchService {
    private final MatchRepository matchRepository;
    private final RiotApiService riotApiService;

    @Transactional
    public MatchResponse importMatch(Long userId, MatchImportRequest request) {
        // 1. 매치 생성
        Match match = createMatch(userId, request);

        // 2. 비동기 처리
        riotApiService.importMatchAsync(match, request.getMatchId());

        return MatchResponse.from(match);
    }
}
```

### 3. Repository Layer

**역할:** 데이터베이스 접근

**주요 인터페이스:**
- `UserRepository`
- `MatchRepository`
- `AnalysisRepository`
- `HighlightRepository`

**예시:**
```java
public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByMatchId(String matchId);
    Page<Match> findByUserId(Long userId, Pageable pageable);
    boolean existsByMatchId(String matchId);
    List<Match> findTop20ByUserIdOrderByGameCreationDesc(Long userId);
}
```

---

## 도메인 모델

### User (사용자)

```java
@Entity
public class User extends BaseEntity {
    private String email;
    private String name;
    private String profileImage;

    // Riot 계정 정보
    private String riotId;
    private String riotPuuid;
    private String summonerName;
    private String riotTagLine;

    // OAuth2
    private AuthProvider provider;
    private String providerId;

    private UserRole role;
}
```

**관계:**
- Match: 1:N
- Analysis: 1:N (through Match)
- Highlight: 1:N (through Match)

### Match (매치)

```java
@Entity
public class Match extends BaseEntity {
    @ManyToOne
    private User user;

    private String matchId;
    private String championName;

    // 게임 통계
    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Double kda;
    private Boolean win;

    // 게임 정보
    private Integer gameDuration;
    private Long gameCreation;

    private MatchStatus status;
    private String timelineData;
}
```

**MatchStatus:**
- `PENDING`: 데이터 가져오는 중
- `COMPLETED`: 완료
- `FAILED`: 실패

### Analysis (분석)

```java
@Entity
public class Analysis extends BaseEntity {
    @ManyToOne
    private Match match;

    private AnalysisStatus status;

    // AI 분석 결과는 별도 테이블 또는 JSON으로 저장
}
```

**AnalysisStatus:**
- `PENDING`: 분석 중
- `COMPLETED`: 완료
- `FAILED`: 실패

### Highlight (하이라이트)

```java
@Entity
public class Highlight extends BaseEntity {
    @ManyToOne
    private Match match;

    private String title;
    private String description;

    // 시간 정보
    private Integer startTime;
    private Integer endTime;
    private Integer duration;

    private HighlightType type;
    private HighlightStatus status;

    // 미디어
    private String videoUrl;
    private String thumbnailUrl;

    private Integer viewCount;
}
```

**HighlightType:**
- `KILL`, `MULTIKILL`, `PENTAKILL`
- `BARON`, `DRAGON`, `TOWER`
- `TEAMFIGHT`, `CUSTOM`

---

## 비동기 처리 아키텍처

### AsyncConfig

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

**ThreadPool 설정:**
- Core Pool Size: 5 (기본 스레드 수)
- Max Pool Size: 10 (최대 스레드 수)
- Queue Capacity: 100 (대기 큐 크기)

### 비동기 처리 흐름

```
┌─────────────┐
│  Controller │
└──────┬──────┘
       │ 1. 요청
       │
┌──────▼──────┐
│   Service   │
└──────┬──────┘
       │ 2. 동기: DB 저장 (PENDING)
       │ 3. 응답 반환
       │
       │ 4. 비동기: @Async 메서드 호출
       │
┌──────▼─────────────────┐
│ Integration Service    │
│ (@Async)               │
└──────┬─────────────────┘
       │ 5. 외부 API 호출
       │
┌──────▼──────┐
│ External    │
│ API         │
└──────┬──────┘
       │ 6. 결과 반환
       │
┌──────▼─────────────────┐
│ Integration Service    │
│ - DB 업데이트          │
│ - status → COMPLETED   │
└────────────────────────┘
```

**예시: 매치 분석**

```java
// AnalysisService.java
@Transactional
public AnalysisResponse createAnalysis(AnalysisCreateRequest request) {
    // 1. PENDING 상태로 저장
    Analysis analysis = analysisRepository.save(
        Analysis.builder()
            .match(match)
            .status(AnalysisStatus.PENDING)
            .build()
    );

    // 2. 비동기 처리 시작
    flaskIntegrationService.processAnalysisAsync(analysis, match);

    // 3. 즉시 응답 반환
    return AnalysisResponse.from(analysis);
}

// FlaskIntegrationService.java
@Async
@Transactional
public void processAnalysisAsync(Analysis analysis, Match match) {
    try {
        // 4. Flask API 호출
        FlaskMatchAnalysisResponse response = flaskApiClient.analyzeMatch(...);

        // 5. 결과 저장
        analysis.updateStatus(AnalysisStatus.COMPLETED);
        analysisRepository.save(analysis);
    } catch (Exception e) {
        // 6. 에러 처리
        analysis.updateStatus(AnalysisStatus.FAILED);
        analysisRepository.save(analysis);
    }
}
```

---

## 외부 API 연동

### Flask AI 서버 연동

**FlaskApiClient:**
```java
@Component
public class FlaskApiClient {
    private final RestTemplate restTemplate;
    private final String flaskBaseUrl;

    public FlaskMatchAnalysisResponse analyzeMatch(
        String matchId,
        String summonerName,
        String tagLine
    ) {
        String url = flaskBaseUrl + "/api/v1/analyze/match";
        // RestTemplate으로 POST 요청
        return restTemplate.postForEntity(url, request, FlaskMatchAnalysisResponse.class)
            .getBody();
    }
}
```

**FlaskIntegrationService:**
```java
@Service
public class FlaskIntegrationService {
    @Async
    @Transactional
    public void processAnalysisAsync(Analysis analysis, Match match) {
        FlaskMatchAnalysisResponse response = flaskApiClient.analyzeMatch(...);
        // 결과 처리
    }
}
```

### Riot API 연동

**RiotApiClient:**
```java
@Component
public class RiotApiClient {
    public RiotMatchResponse getMatchDetails(String matchId) {
        String url = riotBaseUrl + "/lol/match/v5/matches/" + matchId;
        // RestTemplate으로 GET 요청
        return restTemplate.exchange(url, HttpMethod.GET, entity, RiotMatchResponse.class)
            .getBody();
    }
}
```

**RiotApiService:**
```java
@Service
public class RiotApiService {
    @Async
    @Transactional
    public void importMatchAsync(Match match, String matchId) {
        RiotMatchResponse response = riotApiClient.getMatchDetails(matchId);
        // 매치 데이터 업데이트
        match.updateMatchData(...);
        matchRepository.save(match);
    }
}
```

---

## 보안 (Security)

### JWT 인증

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf().disable()
            .authorizeHttpRequests()
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**JwtTokenProvider:**
- JWT 토큰 생성
- JWT 토큰 검증
- 사용자 정보 추출

**JwtAuthenticationFilter:**
- 요청 헤더에서 JWT 토큰 추출
- 토큰 검증 및 인증 정보 설정

### OAuth2 인증 (Google)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
```

**OAuth2AuthenticationSuccessHandler:**
- OAuth2 로그인 성공 시 JWT 토큰 발급
- 사용자 정보 저장

---

## 예외 처리

### GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse response = ErrorResponse.of(e.getErrorCode());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(500).body(response);
    }
}
```

### ErrorCode

```java
public enum ErrorCode {
    // User
    USER_NOT_FOUND(404, "U001", "User not found"),

    // Match
    MATCH_NOT_FOUND(404, "M001", "Match not found"),
    MATCH_ALREADY_EXISTS(409, "M002", "Match already exists"),

    // Analysis
    ANALYSIS_NOT_FOUND(404, "A001", "Analysis not found"),
    ANALYSIS_ALREADY_EXISTS(409, "A002", "Analysis already exists"),

    // Highlight
    HIGHLIGHT_NOT_FOUND(404, "H001", "Highlight not found"),

    // External API
    FLASK_API_ERROR(500, "E001", "Flask API error"),
    RIOT_API_ERROR(500, "E002", "Riot API error");
}
```

---

## 데이터베이스

### 개발 환경 (H2)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver

  h2:
    console:
      enabled: true
      path: /h2-console
```

### 프로덕션 환경 (PostgreSQL - 예정)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lol_highlight
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
```

### JPA 설정

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # 개발: create-drop, 프로덕션: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

---

## 성능 최적화

### 1. N+1 문제 해결

**Fetch Join 사용:**
```java
@Query("SELECT m FROM Match m JOIN FETCH m.user WHERE m.id = :id")
Optional<Match> findByIdWithUser(@Param("id") Long id);
```

**EntityGraph 사용:**
```java
@EntityGraph(attributePaths = {"user"})
Page<Match> findByUserId(Long userId, Pageable pageable);
```

### 2. 페이징

```java
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
Page<Match> matches = matchRepository.findByUserId(userId, pageable);
```

### 3. 비동기 처리

- 외부 API 호출은 모두 비동기로 처리
- ThreadPool로 동시 처리 제한

### 4. 캐싱 (예정)

```java
@Cacheable(value = "matches", key = "#matchId")
public MatchResponse getMatchById(Long matchId) {
    // ...
}
```

---

## 테스트

### Unit Test

```java
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    void getMatchById_Success() {
        // Given
        Match match = createMockMatch();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        // When
        MatchResponse response = matchService.getMatchById(1L);

        // Then
        assertNotNull(response);
        assertEquals("KR_1234567890", response.getMatchId());
    }
}
```

### Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class MatchControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMatch_Success() throws Exception {
        mockMvc.perform(get("/api/matches/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matchId").value("KR_1234567890"));
    }
}
```

---

## 모니터링 (예정)

### Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
```

### 로깅

```yaml
logging:
  level:
    com.lol.highlight: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

---

## 참고 자료

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
