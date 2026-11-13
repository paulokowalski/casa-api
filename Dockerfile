FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-noble
WORKDIR /app

ARG JAR_FILE=/build/target/*.jar
ARG SPRING_PROFILE=prod

ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
ENV TZ=America/Sao_Paulo

COPY --from=build ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-XX:+UseStringDeduplication", \
    "-Xmx512m", \
    "-Xms256m", \
    "-jar", \
    "app.jar" \
]

LABEL maintainer="Paulo Kowalski" \
      version="1.0.0" \
      description="Casa API - Gerenciamento de despesas domésticas"