# ============================================
# 1단계: 의존성 다운로드 (캐시 최적화)
# ============================================
FROM gradle:8.5-jdk17-alpine AS deps

WORKDIR /app

# Gradle 파일만 복사하여 의존성 캐싱
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon --refresh-dependencies || true

# ============================================
# 2단계: 애플리케이션 빌드
# ============================================
FROM gradle:8.5-jdk17-alpine AS build

WORKDIR /app

# 의존성 캐시 복사 (재빌드 시 재사용)
COPY --from=deps /root/.gradle /root/.gradle
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (병렬 처리 및 캐싱)
RUN gradle bootJar --no-daemon -x test \
    --build-cache \
    --parallel

# ============================================
# 3단계: 런타임 이미지 (최소 이미지)
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 최소한의 패키지만 설치 및 업데이트
RUN apk add --no-cache curl && \
    apk upgrade --no-cache

# 보안을 위한 non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 빌드 스테이지에서 jar 파일 복사
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# non-root 사용자로 전환
USER spring

# 애플리케이션 포트 노출
EXPOSE 8080

# Health check 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# JVM 최적화 옵션
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
