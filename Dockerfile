# --- Build stage ---
FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:11-jre-alpine
RUN addgroup -S app && adduser -S app -G app && apk add --no-cache tzdata curl
USER app
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --from=build /workspace/${JAR_FILE} app.jar

ENV TZ=UTC \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8090

EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -fs http://localhost:8090/actuator/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]