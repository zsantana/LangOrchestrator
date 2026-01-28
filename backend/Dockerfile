# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar apenas pom.xml primeiro para aproveitar o cache do Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar o código fonte
COPY src ./src

# Build da aplicação
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criar diretório de saída
RUN mkdir -p /app/output

# Copiar JAR da build stage
COPY --from=build /app/target/*.jar app.jar

# Variáveis de ambiente
ENV SPRING_PROFILES_ACTIVE=prod
ENV ANTHROPIC_API_KEY=""
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Executar aplicação
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
