# 멀티 스테이지 빌드로 최적화된 Spring Boot 애플리케이션
FROM gradle:8.5-jdk17-alpine AS build

WORKDIR /app

# Gradle 파일을 먼저 복사하여 의존성 캐싱
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN gradle bootJar --no-daemon -x test

# 런타임 스테이지 - 최소 이미지 사용
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Health check용 curl 및 타임존 데이터 설치
RUN apk add --no-cache curl tzdata

# 한국 시간대 설정
ENV TZ=Asia/Seoul

# 보안을 위한 non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 빌드 스테이지에서 jar 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# non-root 사용자로 소유권 변경
RUN chown -R spring:spring /app

# non-root 사용자로 전환
USER spring

# 애플리케이션 포트 노출
EXPOSE 8080

# Health check 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# 프로덕션 프로파일로 애플리케이션 실행
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
