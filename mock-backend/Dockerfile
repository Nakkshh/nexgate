# ── Build stage ───────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S nexgate && adduser -S nexgate -G nexgate
COPY --from=build /app/target/*.jar app.jar
USER nexgate
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
