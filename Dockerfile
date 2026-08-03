# 1. Java 17 및 Gradle 빌드 환경
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew build -x test --no-daemon

# 2. 실행 환경 (openjdk:17-slim 대신 eclipse-temurin 사용)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]