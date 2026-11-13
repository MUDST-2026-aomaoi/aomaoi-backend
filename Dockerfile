# Stage 1: Build with Maven
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
# Download dependencies first (cache layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ src/
FROM builder AS test
RUN ./mvnw verify -B

FROM builder AS package
RUN ./mvnw package -DskipTests -B

# Stage 2: Run with lightweight JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid 10001 --no-create-home app
RUN mkdir -p /var/log/aomaoi && chown 10001:10001 /var/log/aomaoi
COPY --from=package --chown=10001:10001 /app/target/*.jar app.jar
USER 10001:10001
# Small hosts (e.g. Render's 0.1-CPU instances) spend most of startup in the C2 JIT compiler.
# C1-only cuts startup time noticeably; override JAVA_TOOL_OPTIONS to change it (k8s does).
ENV JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
