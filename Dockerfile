# Этап 1: Компиляция
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY SentimentApp.java .
RUN javac SentimentApp.java

# Этап 2: Создание минимальной JRE только с нужными модулями
FROM eclipse-temurin:17-jdk-alpine AS jre-build
RUN jlink \
    --add-modules java.base,jdk.httpserver \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /jre

# Этап 3: Финальный сверхлёгкий образ
FROM alpine:latest
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /jre $JAVA_HOME
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/SentimentApp.class .
USER app
EXPOSE 8080
ENTRYPOINT ["java", "SentimentApp"]
