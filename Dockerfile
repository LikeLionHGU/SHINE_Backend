# ---------- 빌드 ----------
FROM gradle:8.14-jdk21 AS builder
WORKDIR /build

# 의존성을 먼저 받아 캐싱한다. 소스만 바뀌면 이 단계를 건너뛴다.
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ---------- 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 서버 메모리가 4GB라 힙을 제한한다. 안 하면 컨테이너가 죽을 수 있다.
ENV JAVA_OPTS="-Xms256m -Xmx1024m -Duser.timezone=Asia/Seoul"

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
