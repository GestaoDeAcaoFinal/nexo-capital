# syntax=docker/dockerfile:1

# Etapa 1: compila o projeto e gera o arquivo JAR.
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Copiar o pom antes do codigo permite reutilizar o cache das dependencias.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -DskipTests package

# Etapa 2: imagem final, contendo somente Java e a aplicacao compilada.
FROM eclipse-temurin:17-jre-jammy AS runtime

ARG UID=10001
WORKDIR /app

# A aplicacao nao precisa executar como o usuario root do container.
RUN useradd --uid "${UID}" --no-create-home --shell /usr/sbin/nologin appuser

COPY --from=build --chown=appuser:appuser /workspace/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
