# mesma imagem dos testes do commit e do e2e (maven:3.9-eclipse-temurin-25)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# cache do Maven entre builds (BuildKit): as dependências não são baixadas de novo a cada deploy
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:25-jre-noble
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
      description="Casa API - Gerenciamento de despesas domésticas"
