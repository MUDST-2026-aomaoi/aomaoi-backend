# Stage 1: Build with Maven
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
# Download dependencies first (cache layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Stage 2: Run with lightweight JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
