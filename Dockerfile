# Build stage
FROM maven:3.8.3-openjdk-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Argumentos com valores padrão
ARG JAR_FILE=/build/target/*.jar
ARG SPRING_PROFILE=prod

# Variáveis de ambiente
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
ENV TZ=America/Sao_Paulo

# Copiar o JAR do estágio de build
COPY --from=build ${JAR_FILE} app.jar

# Expor a porta da aplicação
EXPOSE 8080

# Configurar ponto de entrada com opções otimizadas da JVM
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-XX:+UseStringDeduplication", \
    "-Xmx512m", \
    "-Xms256m", \
    "-jar", \
    "app.jar" \
]

# Metadata
LABEL maintainer="Paulo Kowalski" \
      version="1.0.0" \
      description="Casa API - Gerenciamento de despesas domésticas"