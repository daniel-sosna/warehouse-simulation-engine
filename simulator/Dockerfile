FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar
COPY data ./data
COPY build ./build
RUN chmod +x build/router/router-linux-amd64

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--dataDir", "./data/1", "--router", "build/router/router-linux-amd64", "--eventLogFile", "./output_logs.json"]
