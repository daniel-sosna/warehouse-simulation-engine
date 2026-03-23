# ---------- BUILD ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy only pom first (better caching)
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean -DskipTests package

# ---------- RUNTIME ----------
FROM eclipse-temurin:21-jre-alpine

# App lives here (NOT mounted)
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# User workspace
WORKDIR /work

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
