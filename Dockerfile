# 1. Spring Boot 빌드
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon


# 2. 실제 실행용 이미지
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]