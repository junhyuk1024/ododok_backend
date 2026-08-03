# 1. Java 17 및 Gradle 빌드 환경
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 상세 로그(--stacktrace --info)를 포함하여 빌드 실행
RUN ./gradlew clean build -x test --stacktrace --info --no-daemon

# 2. 실행 환경
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]