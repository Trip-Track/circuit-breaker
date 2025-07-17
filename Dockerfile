FROM gradle:8.5.0-jdk21 AS build

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

COPY src ./src

RUN gradle installDist

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/build/install/ /app

EXPOSE 7777

ENV PORT=7777

CMD ["./circuit-breaker/bin/circuit-breaker"]
